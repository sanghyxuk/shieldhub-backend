package com.shieldhub.backend.service;

import com.shieldhub.backend.dto.response.FlaskAnalysisResponse;
import com.shieldhub.backend.entity.*;
import com.shieldhub.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final RestTemplate restTemplate;
    private final UserRepository userRepository;
    private final AnalysisResultsRepository analysisResultsRepository;
    private final VulnerabilitiesRepository vulnerabilitiesRepository;
    private final AnalysisVulnerabilitiesRepository analysisVulnerabilitiesRepository;

    @Value("${app.flask.server-url}")
    private String flaskServerUrl;

    @Transactional
    public FlaskAnalysisResponse startAnalysis(String url, String username) {
        // 1. 사용자 ID 찾기
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        // 2. 분석 요청 기록 (상태: PENDING)
        AnalysisResults analysisRecord = new AnalysisResults();
        analysisRecord.setUrlToAnalyze(url);
        analysisRecord.setUserId(user.getUserId());
        analysisRecord.setStatus(AnalysisResults.AnalysisStatus.PENDING);
        AnalysisResults savedRecord = analysisResultsRepository.save(analysisRecord);

        try {
            // 3. Flask AI 서버에 분석 요청
            log.info("Flask 서버로 분석 요청 전송: {}", url);
            String flaskApiUrl = flaskServerUrl + "/api/analyze";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> requestBody = Collections.singletonMap("url", url);
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<FlaskAnalysisResponse> responseEntity = restTemplate.postForEntity(
                    flaskApiUrl,
                    requestEntity,
                    FlaskAnalysisResponse.class
            );

            FlaskAnalysisResponse flaskResponse = responseEntity.getBody();

            if (flaskResponse == null || !flaskResponse.isSuccess()) {
                throw new RuntimeException("Flask 서버 분석 실패: " + flaskResponse.toString());
            }

            // 4. 분석 결과 DB에 저장
            log.info("Flask 분석 완료. 결과 DB 저장 시작...");
            saveFlaskResults(savedRecord, flaskResponse);

            return flaskResponse;

        } catch (Exception e) {
            log.error("Flask 서버 통신 오류: {}", e.getMessage());
            // 5. 오류 발생 시 DB 상태 FAILED로 변경
            savedRecord.setStatus(AnalysisResults.AnalysisStatus.FAILED);
            savedRecord.setErrorMessage(e.getMessage());
            savedRecord.setCompletionDate(LocalDateTime.now());
            analysisResultsRepository.save(savedRecord);
            throw new RuntimeException("AI 서버 통신 중 오류가 발생했습니다.", e);
        }
    }

    @Transactional
    protected void saveFlaskResults(AnalysisResults analysisRecord, FlaskAnalysisResponse flaskResponse) {
        // 1. Vulnerabilities 테이블에 취약점 정보 저장 (중복 확인)
        for (FlaskAnalysisResponse.VulnerabilityDetail detail : flaskResponse.getVulnerabilities()) {

            // 1-1. DB에서 (Type + Pattern)으로 이미 존재하는 취약점인지 확인
            Vulnerabilities vuln = vulnerabilitiesRepository
                    .findByVulnerabilityTypeAndDetectedPattern(detail.getType(), detail.getPattern())
                    .orElseGet(() -> {
                        // 1-2. 없으면 새로 저장
                        Vulnerabilities newVuln = new Vulnerabilities();
                        newVuln.setVulnerabilityType(detail.getType());
                        newVuln.setDetectedPattern(detail.getPattern());

                        // Enum 변환 (대소문자 무시, 예외 처리 추가)
                        try {
                            newVuln.setSeverity(Vulnerabilities.Severity.valueOf(detail.getSeverity().toUpperCase()));
                        } catch (IllegalArgumentException e) {
                            log.warn("알 수 없는 Severity 값: {}. 기본값 HIGH 사용", detail.getSeverity());
                            newVuln.setSeverity(Vulnerabilities.Severity.HIGH); // 기본값
                        }

                        newVuln.setDetails(detail.getDetails()); // 상세 설명
                        return vulnerabilitiesRepository.save(newVuln);
                    });

            // 2. AnalysisVulnerabilities (중간 테이블)에 매핑 정보 저장
            AnalysisVulnerabilities.AnalysisVulnerabilitiesId relationId =
                    new AnalysisVulnerabilities.AnalysisVulnerabilitiesId(
                            analysisRecord.getAnalysisId(),
                            vuln.getVulnerabilityId()
                    );

            AnalysisVulnerabilities relation = new AnalysisVulnerabilities(relationId, detail.getDetails());
            analysisVulnerabilitiesRepository.save(relation);
        }

        // 3. AnalysisResults 상태 COMPLETED로 업데이트
        analysisRecord.setStatus(AnalysisResults.AnalysisStatus.COMPLETED);
        analysisRecord.setCompletionDate(LocalDateTime.now());
        analysisResultsRepository.save(analysisRecord);
    }
}
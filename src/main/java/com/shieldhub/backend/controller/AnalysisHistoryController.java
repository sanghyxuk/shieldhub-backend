package com.shieldhub.backend.controller;

import com.shieldhub.backend.entity.AnalysisResults;
import com.shieldhub.backend.entity.AnalysisVulnerabilities;
import com.shieldhub.backend.repository.AnalysisResultsRepository;
import com.shieldhub.backend.repository.AnalysisVulnerabilitiesRepository;
import com.shieldhub.backend.repository.VulnerabilitiesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisHistoryController {
    private final AnalysisResultsRepository analysisResultsRepository;
    private final AnalysisVulnerabilitiesRepository analysisVulnerabilitiesRepository;
    private final VulnerabilitiesRepository vulnerabilitiesRepository;

    // 분석 이력 목록 조회 (페이징)
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "analysisDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort sort = "asc".equalsIgnoreCase(direction) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<AnalysisResults> p = analysisResultsRepository.findAll(pageable);

        Map<String, Object> resp = new HashMap<>();
        resp.put("page", p.getNumber());
        resp.put("size", p.getSize());
        resp.put("totalElements", p.getTotalElements());
        resp.put("totalPages", p.getTotalPages());
        resp.put("items", p.stream().map(ar -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", ar.getAnalysisId());
            m.put("targetUrl", ar.getUrlToAnalyze());
            m.put("status", ar.getStatus());
            m.put("startedAt", ar.getAnalysisDate());
            m.put("finishedAt", ar.getCompletionDate());
            m.put("errorMessage", ar.getErrorMessage());
            return m;
        }).collect(Collectors.toList()));
        return ResponseEntity.ok(resp);
    }

    // 분석 이력 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable Integer id) {
        Optional<AnalysisResults> opt = analysisResultsRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "timestamp", new Date(),
                    "status", 404,
                    "error", "Not Found",
                    "message", "Scan not found: " + id,
                    "path", "/api/analysis/" + id
            ));
        }
        AnalysisResults ar = opt.get();
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", ar.getAnalysisId());
        dto.put("targetUrl", ar.getUrlToAnalyze());
        dto.put("status", ar.getStatus());
        dto.put("startedAt", ar.getAnalysisDate());
        dto.put("finishedAt", ar.getCompletionDate());
        dto.put("errorMessage", ar.getErrorMessage());

        // 연관 취약점 조회
        List<AnalysisVulnerabilities> relations = analysisVulnerabilitiesRepository.findByIdAnalysisId(ar.getAnalysisId());
        List<Map<String, Object>> vulns = relations.stream().map(rel -> {
            Integer vulnId = rel.getId().getVulnerabilityId();
            Map<String, Object> vm = new HashMap<>();
            vm.put("vulnerabilityId", vulnId);
            vm.put("detectionContext", rel.getDetectionContext());
            vulnerabilitiesRepository.findById(vulnId).ifPresent(v -> {
                vm.put("type", v.getVulnerabilityType());
                vm.put("severity", v.getSeverity());
                vm.put("pattern", v.getDetectedPattern());
                vm.put("details", v.getDetails());
            });
            return vm;
        }).collect(Collectors.toList());
        dto.put("vulnerabilities", vulns);
        return ResponseEntity.ok(dto);
    }
}

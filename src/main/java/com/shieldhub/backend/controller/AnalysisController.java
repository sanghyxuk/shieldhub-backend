package com.shieldhub.backend.controller;

import com.shieldhub.backend.dto.request.AnalysisRequest;
import com.shieldhub.backend.dto.response.FlaskAnalysisResponse;
import com.shieldhub.backend.service.AnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping("/start")
    public ResponseEntity<?> startAnalysis(
            @Valid @RequestBody AnalysisRequest request,
            Authentication authentication) {

        try {
            String username = authentication.getName(); // 인증된 사용자
            FlaskAnalysisResponse result = analysisService.startAnalysis(request.getUrl(), username);

            // Flask의 응답을 React에 그대로 전달
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
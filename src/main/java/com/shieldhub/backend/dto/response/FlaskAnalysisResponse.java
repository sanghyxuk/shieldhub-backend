package com.shieldhub.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

// Flask가 보내는 JSON과 매핑되는 DTO
// Flask에 없는 필드가 있어도 무시하도록 설정
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlaskAnalysisResponse {
    private boolean success;
    private String url;
    private int vulnerability_count;
    private List<VulnerabilityDetail> vulnerabilities;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VulnerabilityDetail {
        private String type;
        private String severity;
        private String pattern;
        private String details; // scanner.py의 'details' (detection_context)
        private double confidence;
    }
}
package com.shieldhub.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnalysisRequest {
    @NotBlank(message = "분석할 URL은 필수입니다.")
    private String url;
}
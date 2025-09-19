package com.shieldhub.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OtpSetupResponse {
    private String secret;
    private String qrCodeUrl;
    private String qrCodeImage;  // Base64 인코딩된 이미지
}
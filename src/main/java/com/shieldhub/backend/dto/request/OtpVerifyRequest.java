package com.shieldhub.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OtpVerifyRequest {

    @NotNull(message = "OTP 코드는 필수입니다")
    private Integer code;
}
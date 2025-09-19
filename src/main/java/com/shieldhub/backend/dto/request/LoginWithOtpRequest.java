package com.shieldhub.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoginWithOtpRequest {

    @NotBlank(message = "사용자 이름은 필수입니다")
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;

    @NotNull(message = "OTP 코드는 필수입니다")
    private Integer otpCode;
}
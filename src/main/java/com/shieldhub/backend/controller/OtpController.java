package com.shieldhub.backend.controller;

import com.shieldhub.backend.dto.request.OtpVerifyRequest;
import com.shieldhub.backend.dto.response.OtpSetupResponse;
import com.shieldhub.backend.entity.User;  // 이 import 추가
import com.shieldhub.backend.repository.UserRepository;  // 이 import 추가
import com.shieldhub.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/otp")
@RequiredArgsConstructor
public class OtpController {

    private final AuthService authService;
    private final UserRepository userRepository;

    /**
     * OTP 설정 시작 (QR 코드 생성)
     */
    @PostMapping("/setup")
    public ResponseEntity<?> setupOtp(Authentication authentication) {
        try {
            String username = authentication.getName();
            OtpSetupResponse response = authService.setupOtp(username);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * OTP 활성화 (코드 검증 후)
     */
    @PostMapping("/enable")
    public ResponseEntity<?> enableOtp(
            @RequestBody OtpVerifyRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            authService.enableOtp(username, request.getCode());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "OTP가 활성화되었습니다");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * OTP 비활성화
     */
    @PostMapping("/disable")
    public ResponseEntity<?> disableOtp(
            @RequestBody OtpVerifyRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            authService.disableOtp(username, request.getCode());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "OTP가 비활성화되었습니다");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getOtpStatus(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            Map<String, Object> response = new HashMap<>();
            response.put("otpEnabled", user.getIsOtpEnabled());
            response.put("hasSecret", user.getOtpSecret() != null);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

}
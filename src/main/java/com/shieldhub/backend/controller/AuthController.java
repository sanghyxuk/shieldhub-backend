package com.shieldhub.backend.controller;

import com.shieldhub.backend.dto.request.LoginRequest;
import com.shieldhub.backend.dto.request.RegisterRequest;
import com.shieldhub.backend.dto.request.ChangePasswordRequest;
import com.shieldhub.backend.dto.request.LoginWithOtpRequest;  // 이 줄 추가
import com.shieldhub.backend.dto.response.LoginResponse;
import com.shieldhub.backend.entity.User;
import com.shieldhub.backend.repository.UserRepository;  // 이 줄도 추가 (check-otp 엔드포인트에 필요)
import com.shieldhub.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = authService.register(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "회원가입이 완료되었습니다");
            response.put("userId", user.getUserId());
            response.put("username", user.getUsername());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // 로그아웃 (클라이언트에서 토큰 삭제하면 됨)
    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication authentication) {
        try {
            String username = authentication.getName();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "로그아웃되었습니다");

            // TODO: Redis에 블랙리스트로 토큰 추가 (선택사항)

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "로그아웃 실패");

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }


    // 비밀번호 변경
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            authService.changePassword(username, request.getCurrentPassword(), request.getNewPassword());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "비밀번호가 변경되었습니다");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // 비밀번호 재설정 (임시 비밀번호 발급)
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String tempPassword = authService.resetPassword(email);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "임시 비밀번호가 이메일로 발송되었습니다");
            response.put("tempPassword", tempPassword); // 개발용 (실제론 삭제)

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // 회원 탈퇴
    @DeleteMapping("/delete-account")
    public ResponseEntity<?> deleteAccount(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            String password = request.get("password");

            authService.deleteUser(username, password);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "회원 탈퇴가 완료되었습니다");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/login-with-otp")
    public ResponseEntity<?> loginWithOtp(@Valid @RequestBody LoginWithOtpRequest request) {
        try {
            LoginResponse response = authService.loginWithOtp(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * OTP 활성화 여부 확인 (로그인 전)
     */
    @PostMapping("/check-otp")
    public ResponseEntity<?> checkOtpEnabled(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            Map<String, Object> response = new HashMap<>();
            response.put("otpEnabled", user.getIsOtpEnabled());
            response.put("username", username);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

}
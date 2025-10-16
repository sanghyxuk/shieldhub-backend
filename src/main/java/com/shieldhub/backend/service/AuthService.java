package com.shieldhub.backend.service;

import com.shieldhub.backend.dto.request.LoginRequest;
import com.shieldhub.backend.dto.request.RegisterRequest;
import com.shieldhub.backend.dto.request.LoginWithOtpRequest;  // 이 줄 추가
import com.shieldhub.backend.dto.response.LoginResponse;
import com.shieldhub.backend.dto.response.OtpSetupResponse;  // 이 줄 추가
import lombok.extern.slf4j.Slf4j;
import com.shieldhub.backend.entity.User;
import com.shieldhub.backend.repository.UserRepository;
import com.shieldhub.backend.util.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.shieldhub.backend.dto.request.FindIdRequest;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;

    // 회원가입
    public User register(RegisterRequest request) {
        // 이미 존재하는 사용자명 확인
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("이미 사용 중인 사용자명입니다");
        }

        // 이메일 중복 확인
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 사용 중인 이메일입니다");
        }

        // User 엔티티 생성
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setEmail(request.getEmail());
        user.setIsOtpEnabled(false);

        return userRepository.save(user);
    }

    // 아이디(username) 찾기
    @Transactional(readOnly = true) // 데이터를 변경하지 않으므로 readOnly 속성 추가
    public String findUsername(FindIdRequest request) {
        // 이메일과 전화번호 둘 다 없는 경우 예외 처리
        if (request.getEmail() == null && request.getPhoneNumber() == null) {
            throw new RuntimeException("이메일 또는 전화번호를 입력해주세요.");
        }

        Optional<User> userOptional;

        // 이메일이 존재하면 이메일로 사용자 조회
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            userOptional = userRepository.findByEmail(request.getEmail());
        }
        // 이메일이 없고 전화번호가 존재하면 전화번호로 조회
        else {
            userOptional = userRepository.findByPhoneNumber(request.getPhoneNumber());
        }

        // 사용자를 찾았으면 username 반환, 없으면 예외 처리
        return userOptional
                .map(User::getUsername)
                .orElseThrow(() -> new RuntimeException("해당 정보로 가입된 사용자를 찾을 수 없습니다."));
    }

    // 로그인
    public LoginResponse login(LoginRequest request) {
        // 사용자 조회
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다");
        }

        // OTP가 활성화된 경우 일반 로그인 차단
        if (user.getIsOtpEnabled()) {
            throw new RuntimeException("OTP 2단계 인증이 활성화되어 있습니다. OTP 코드를 포함하여 로그인해주세요.");
        }

        // JWT 토큰 생성
        String token = jwtTokenUtil.generateToken(user.getUsername());

        return new LoginResponse(token, user.getUserId(), user.getUsername(), user.getName());
    }

    // 비밀번호 변경
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다");
        }

        // 새 비밀번호로 변경
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // 회원 탈퇴
    public void deleteUser(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        // 비밀번호 확인
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다");
        }

        userRepository.delete(user);
    }

    // 비밀번호 재설정 (이메일로 임시 비밀번호 발송)
    public String resetPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("해당 이메일의 사용자를 찾을 수 없습니다"));

        // 임시 비밀번호 생성 (8자리 랜덤)
        String tempPassword = generateTempPassword();

        // 임시 비밀번호로 변경
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        // 이메일 발송
        try {
            emailService.sendPasswordResetEmail(email, user.getUsername(), tempPassword);
        } catch (Exception e) {
            log.error("비밀번호 재설정 이메일 발송 실패: {}", e.getMessage());
            throw new RuntimeException("이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }

        return "이메일로 임시 비밀번호가 발송되었습니다";  // 실제 비밀번호는 반환하지 않음
    }

    // 임시 비밀번호 생성
    private String generateTempPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder temp = new StringBuilder();
        java.util.Random random = new java.util.Random();

        for (int i = 0; i < 8; i++) {
            temp.append(chars.charAt(random.nextInt(chars.length())));
        }

        return temp.toString();
    }

    /**
     * OTP 설정 초기화 (QR 코드 생성)
     */
    public OtpSetupResponse setupOtp(String username) throws Exception {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        // 이미 OTP가 활성화되어 있으면 에러
        if (user.getIsOtpEnabled()) {
            throw new RuntimeException("OTP가 이미 활성화되어 있습니다");
        }

        // 새로운 비밀키 생성
        String secret = otpService.generateSecretKey();

        // QR 코드 URL 생성
        String qrCodeUrl = otpService.generateQrCodeUrl(username, secret);

        // QR 코드 이미지 생성
        String qrCodeImage = otpService.generateQrCodeImage(qrCodeUrl);

        // 비밀키를 DB에 임시 저장 (아직 활성화 안됨)
        user.setOtpSecret(secret);
        userRepository.save(user);

        return new OtpSetupResponse(secret, qrCodeUrl, qrCodeImage);
    }

    /**
     * OTP 활성화 (사용자가 코드를 확인한 후)
     */
    public void enableOtp(String username, int verificationCode) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        if (user.getOtpSecret() == null) {
            throw new RuntimeException("OTP 설정이 초기화되지 않았습니다");
        }

        // 검증 코드 확인
        if (!otpService.verifyCode(user.getOtpSecret(), verificationCode)) {
            throw new RuntimeException("OTP 코드가 일치하지 않습니다");
        }

        // OTP 활성화
        user.setIsOtpEnabled(true);
        userRepository.save(user);
    }

    /**
     * OTP 비활성화
     */
    public void disableOtp(String username, int verificationCode) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        if (!user.getIsOtpEnabled()) {
            throw new RuntimeException("OTP가 활성화되어 있지 않습니다");
        }

        // 검증 코드 확인
        if (!otpService.verifyCode(user.getOtpSecret(), verificationCode)) {
            throw new RuntimeException("OTP 코드가 일치하지 않습니다");
        }

        // OTP 비활성화
        user.setIsOtpEnabled(false);
        user.setOtpSecret(null);
        userRepository.save(user);
    }

    /**
     * OTP를 포함한 로그인
     */
    public LoginResponse loginWithOtp(LoginWithOtpRequest request) {
        // 사용자 조회
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다");
        }

        // OTP가 활성화되어 있는지 확인
        if (!user.getIsOtpEnabled()) {
            throw new RuntimeException("OTP가 활성화되어 있지 않습니다");
        }

        // OTP 코드 검증
        if (!otpService.verifyCode(user.getOtpSecret(), request.getOtpCode())) {
            throw new RuntimeException("OTP 코드가 일치하지 않습니다");
        }

        // JWT 토큰 생성
        String token = jwtTokenUtil.generateToken(user.getUsername());

        return new LoginResponse(token, user.getUserId(), user.getUsername(), user.getName());
    }

}
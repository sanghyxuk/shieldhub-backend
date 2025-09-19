package com.shieldhub.backend.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();

    /**
     * OTP 비밀키 생성
     */
    public String generateSecretKey() {
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        return key.getKey();
    }

    /**
     * QR 코드 URL 생성
     */
    public String generateQrCodeUrl(String username, String secret) {
        // GoogleAuthenticatorKey 객체 생성
        GoogleAuthenticatorKey.Builder builder = new GoogleAuthenticatorKey.Builder(secret);
        GoogleAuthenticatorKey credentials = builder.build();

        return GoogleAuthenticatorQRGenerator.getOtpAuthURL(
                "ShieldHub",  // 앱에 표시될 이름
                username,      // 사용자명
                credentials
        );
    }

    /**
     * QR 코드 이미지를 Base64로 생성
     */
    public String generateQrCodeImage(String qrCodeUrl) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BitMatrix matrix = new MultiFormatWriter().encode(
                qrCodeUrl,
                BarcodeFormat.QR_CODE,
                300,  // width
                300   // height
        );

        MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
        byte[] imageBytes = baos.toByteArray();

        return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
    }

    /**
     * OTP 코드 검증
     */
    public boolean verifyCode(String secret, int code) {
        return googleAuthenticator.authorize(secret, code);
    }

    /**
     * 현재 유효한 OTP 코드 생성 (테스트용)
     */
    public int getTotpPassword(String secret) {
        return googleAuthenticator.getTotpPassword(secret);
    }
}
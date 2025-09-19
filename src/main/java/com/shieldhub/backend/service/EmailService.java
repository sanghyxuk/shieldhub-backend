package com.shieldhub.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@shieldhub.com}")
    private String fromEmail;

    @Value("${app.mail.from-name:ShieldHub Security}")
    private String fromName;

    /**
     * 간단한 텍스트 이메일 발송
     */
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            log.info("이메일 발송 완료: {}", to);
        } catch (Exception e) {
            log.error("이메일 발송 실패: {}", e.getMessage());
            throw new RuntimeException("이메일 발송에 실패했습니다", e);
        }
    }

    /**
     * HTML 이메일 발송
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("HTML 이메일 발송 완료: {}", to);
        } catch (MessagingException e) {
            log.error("이메일 발송 실패: {}", e.getMessage());
            throw new RuntimeException("이메일 발송에 실패했습니다", e);
        } catch (Exception e) {
            log.error("이메일 발송 중 오류: {}", e.getMessage());
            throw new RuntimeException("이메일 발송에 실패했습니다", e);
        }
    }

    /**
     * 비밀번호 재설정 이메일 발송
     */
    public void sendPasswordResetEmail(String to, String username, String tempPassword) {
        String subject = "[ShieldHub] 임시 비밀번호 발급 안내";

        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c3e50;">임시 비밀번호 발급 안내</h2>
                    <p>안녕하세요, <strong>%s</strong>님</p>
                    <p>요청하신 임시 비밀번호가 발급되었습니다.</p>
                    
                    <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;">
                        <p style="margin: 0;"><strong>임시 비밀번호:</strong></p>
                        <p style="font-size: 24px; color: #e74c3c; margin: 10px 0; font-weight: bold;">%s</p>
                    </div>
                    
                    <p><strong>⚠️ 보안 안내:</strong></p>
                    <ul>
                        <li>로그인 후 반드시 비밀번호를 변경해주세요.</li>
                        <li>임시 비밀번호는 타인에게 노출되지 않도록 주의하세요.</li>
                        <li>본인이 요청하지 않은 경우 즉시 고객센터로 문의하세요.</li>
                    </ul>
                    
                    <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                    
                    <p style="color: #7f8c8d; font-size: 12px;">
                        본 메일은 발신 전용입니다. 문의사항은 고객센터를 이용해주세요.<br>
                        © 2025 ShieldHub. All rights reserved.
                    </p>
                </div>
            </body>
            </html>
            """, username, tempPassword);

        sendHtmlEmail(to, subject, htmlContent);
    }

    /**
     * 회원가입 환영 이메일 (선택사항)
     */
    public void sendWelcomeEmail(String to, String username) {
        String subject = "[ShieldHub] 회원가입을 환영합니다!";

        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2>ShieldHub 회원가입을 환영합니다!</h2>
                    <p><strong>%s</strong>님, ShieldHub에 가입해주셔서 감사합니다.</p>
                    <p>파일 암호화와 웹 보안 분석 서비스를 안전하게 이용하세요.</p>
                </div>
            </body>
            </html>
            """, username);

        sendHtmlEmail(to, subject, htmlContent);
    }
}
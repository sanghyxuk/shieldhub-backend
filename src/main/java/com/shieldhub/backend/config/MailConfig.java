package com.shieldhub.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // [핵심 수정 1] YAML 파일(@Value) 대신 직접 값을 넣습니다.
        mailSender.setHost("smtp-relay.brevo.com");
        mailSender.setPort(587);

        // [핵심 수정 2] 이메일은 공개되어도 되니 여기에 직접 적습니다.
        mailSender.setUsername("devhyxuk@gmail.com");

        // [핵심 수정 3] 비밀번호는 Render 환경 변수(OS 시스템 변수)에서 직접 가져옵니다.
        // application.yml이 망가져도 이건 작동합니다.
        String mailPassword = System.getenv("MAIL_PASSWORD");
        if (mailPassword == null || mailPassword.isEmpty()) {
            // 로컬 테스트 등을 위한 안전장치 (필요 시 수정)
            mailPassword = "your-local-password-or-dummy";
        }
        mailSender.setPassword(mailPassword);

        mailSender.setDefaultEncoding("UTF-8");

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.debug", "true");

        // 타임아웃 설정 (10초)
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        return mailSender;
    }
}
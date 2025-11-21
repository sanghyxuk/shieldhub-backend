package com.shieldhub.backend.config;

import com.shieldhub.backend.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value; // 환경 변수 읽기용
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration; // CORS 설정용
import org.springframework.web.cors.CorsConfigurationSource; // CORS 설정용
import org.springframework.web.cors.UrlBasedCorsConfigurationSource; // CORS 설정용

import java.util.Arrays; // 리스트 생성용
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // [핵심 수정 1] Render 환경 변수 'FRONTEND_URL'을 가져옵니다.
    // 값이 없으면 기본적으로 로컬호스트를 사용합니다.
    @Value("${FRONTEND_URL:http://localhost:3000}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // [핵심 수정 2] 아래에서 만든 corsConfigurationSource 빈을 사용하도록 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll() // /auth/** 대신 /api/auth/** 로 통일 필요 (프론트 요청 경로 확인)
                        .requestMatchers("/auth/**").permitAll()     // 혹시 몰라 둘 다 허용
                        .requestMatchers("/api/test/**").permitAll()
                        .requestMatchers("/api/otp/**").authenticated()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/files/**").authenticated()
                        .requestMatchers("/api/analysis/**").authenticated()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // [핵심 수정 3] CORS 설정을 정의하는 Bean 추가
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 프론트엔드 주소 목록 (로컬 + 배포된 Vercel 주소)
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                frontendUrl // Render 환경 변수에서 가져온 값 (https://frontend-chi-lake-23.vercel.app)
        ));

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 인증 정보(쿠키/JWT) 허용
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // DaoAuthenticationProvider 생성자 수정 (기본 생성자 사용 후 세터 주입이 안전함)
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setPasswordEncoder(passwordEncoder());
        authProvider.setUserDetailsService(userDetailsService);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
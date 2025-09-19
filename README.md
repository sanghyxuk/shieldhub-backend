ShieldHub Backend
📋 프로젝트 소개
ShieldHub는 파일 보안과 웹 취약점 분석을 제공하는 통합 보안 솔루션의 백엔드 서버입니다. AES-256 암호화 기반의 파일 보안 시스템과 JWT 기반 인증, OTP/2FA를 통한 강력한 보안 체계를 제공합니다.
✨ 주요 기능
🔐 보안 인증 시스템

JWT 기반 토큰 인증
BCrypt 비밀번호 암호화
OTP/2FA (Google Authenticator 연동)
회원가입/로그인/로그아웃
비밀번호 변경 및 재설정
이메일 기반 임시 비밀번호 발급

📁 파일 보안 시스템

AES-256-GCM 암호화
이중 암호화 (파일 키 + 마스터 키)
SHA-256 무결성 검증
ZIP 패키징 (암호화 파일 + 키 + 정보)
파일 암호화/복호화 이력 관리
사용자별 파일 격리

🛠 기술 스택
Backend

Java 17 - Spring Boot 3.2.x
Spring Security 6.x - 인증/인가
Spring Data JPA - ORM
jjwt 0.11.5 - JWT 토큰

Database

MySQL 8.0 - 관계형 데이터베이스
Redis 7 - 세션 관리 (예정)

Security

AES-256-GCM - 파일 암호화
SHA-256 - 해시 함수
BCrypt - 비밀번호 암호화
Google Authenticator - OTP/2FA

DevOps

Docker & Docker Compose - 컨테이너화
Maven - 빌드 도구

🚀 시작하기
사전 요구사항

Java 17
Docker & Docker Compose
Maven

1. 저장소 클론
   bashgit clone https://github.com/yourusername/shieldhub-backend.git
   cd shieldhub-backend
2. 환경 설정
   bash# application-example.yml을 복사하여 application.yml 생성
   cp src/main/resources/application-example.yml src/main/resources/application.yml

# application.yml에서 다음 값들을 변경:
# - app.jwt.secret: 256비트 랜덤 문자열
# - app.encryption.master-key: 256비트 랜덤 문자열
# - spring.datasource.password: 실제 DB 비밀번호
3. Docker 환경 시작
   bash# MySQL + Redis 컨테이너 시작
   docker-compose up -d

# 로그 확인
docker-compose logs -f mysql
4. 애플리케이션 실행
   bashmvn clean install
   mvn spring-boot:run
5. 테스트
   bash# 서버 상태 확인
   curl http://localhost:8080/api/test/health
   📡 API 문서
   인증 API
   MethodEndpoint설명인증 필요POST/api/auth/register회원가입❌POST/api/auth/login로그인❌POST/api/auth/login-with-otpOTP 로그인❌POST/api/auth/check-otpOTP 활성화 확인❌POST/api/auth/logout로그아웃✅PUT/api/auth/change-password비밀번호 변경✅POST/api/auth/reset-password비밀번호 재설정❌DELETE/api/auth/delete-account회원 탈퇴✅
   OTP/2FA API
   MethodEndpoint설명인증 필요POST/api/otp/setupOTP 설정 (QR 코드 생성)✅POST/api/otp/enableOTP 활성화✅POST/api/otp/disableOTP 비활성화✅GET/api/otp/statusOTP 상태 확인✅
   파일 보안 API
   MethodEndpoint설명인증 필요POST/api/files/encrypt파일 암호화✅POST/api/files/decrypt-upload파일 복호화 (업로드)✅POST/api/files/decrypt/{fileId}파일 복호화 (DB)✅GET/api/files/list파일 목록 조회✅
   상세한 API 문서는 API 테스트 가이드를 참조하세요.
   📂 프로젝트 구조
   shieldhub-backend/
   ├── src/main/java/com/shieldhub/backend/
   │   ├── config/          # Spring Security, JWT 필터 설정
   │   ├── controller/      # REST API 컨트롤러
   │   ├── dto/            # 데이터 전송 객체
   │   ├── entity/         # JPA 엔티티
   │   ├── repository/     # 데이터 접근 계층
   │   ├── service/        # 비즈니스 로직
   │   └── util/           # 유틸리티 (암호화, JWT)
   ├── src/main/resources/
   │   └── application.yml # 애플리케이션 설정
   ├── docker-compose.yml  # Docker 설정
   ├── sql/init.sql       # DB 초기화 스크립트
   └── data/              # 암호화 파일 저장소
   🔒 보안 기능
   파일 암호화 플로우

파일 업로드 → 바이트 배열 변환
SHA-256 해시값 생성 (원본 무결성)
랜덤 AES-256 키 생성
파일 AES-GCM 암호화 (IV 포함)
UUID 파일명으로 디스크 저장
파일 키를 마스터키로 재암호화
ZIP 패키징 (암호화파일 + key.txt + info.txt)
메타데이터/이력 DB 저장

인증 플로우

사용자 로그인 → username/password 검증
BCrypt로 비밀번호 확인
JWT 토큰 발급 (24시간 유효)
이후 모든 요청에 Authorization 헤더로 토큰 전송
필터에서 자동 토큰 검증 및 인증 처리

📊 데이터베이스 스키마
Users

사용자 정보 (username, password, email, phone)
OTP 설정 (secret, enabled)

FileMetadata

파일 정보 (이름, 경로, 크기)
SHA-256 해시값
업로드 날짜

FileHistory

암호화/복호화 이력
작업 시간 및 타입

📈 개발 진행 상황
✅ 완료된 기능

Docker 환경 구축 (MySQL, Redis)
Spring Boot 프로젝트 설정
JWT 기반 인증 시스템
Spring Security 통합
회원가입/로그인/로그아웃
비밀번호 변경/재설정
OTP/2FA 구현
파일 암호화/복호화 시스템
SHA-256 무결성 검증
파일 이력 관리

🚧 진행 중인 작업

Flask 머신러닝 API 서버 구축
웹 취약점 분석 기능
Redis 세션 관리

📋 예정된 작업

이메일 인증 시스템 완성
관리자 대시보드
성능 최적화
단위 테스트 작성

🔑 초기 계정
테스트용 계정:
사용자명비밀번호역할adminadmin123관리자testuseruser123일반 사용자
🤝 기여
이 프로젝트는 졸업 작품으로 개발 중입니다.
📄 라이선스
이 프로젝트는 교육 목적으로 개발되었습니다.
👥 개발자

표상혁 - Backend Developer & Security Engineer
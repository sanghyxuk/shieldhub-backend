
-----

# 🛡️ ShieldHub Backend

[](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[](https://spring.io/projects/spring-boot)
[](https://www.mysql.com/)
[](https://www.docker.com/)

> 파일 보안과 웹 취약점 분석을 제공하는 통합 보안 솔루션 백엔드 서버

<br>

## 📚 목차

1.  [프로젝트 소개](https://www.google.com/search?q=%23-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8-%EC%86%8C%EA%B0%9C)
2.  [주요 기능](https://www.google.com/search?q=%23-%EC%A3%BC%EC%9A%94-%EA%B8%B0%EB%8A%A5)
3.  [기술 스택](https://www.google.com/search?q=%23-%EA%B8%B0%EC%88%A0-%EC%8A%A4%ED%83%9D)
4.  [빠른 시작](https://www.google.com/search?q=%23-%EB%B9%A0%EB%A5%B8-%EC%8B%9C%EC%9E%91)
5.  [API 문서](https://www.google.com/search?q=%23-api-%EB%AC%B8%EC%84%9C)
6.  [프로젝트 구조](https://www.google.com/search?q=%23-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8-%EA%B5%AC%EC%A1%B0)
7.  [보안 기능](https://www.google.com/search?q=%23-%EB%B3%B4%EC%95%88-%EA%B8%B0%EB%8A%A5)
8.  [데이터베이스 스키마](https://www.google.com/search?q=%23-%EB%8D%B0%EC%9D%B4%ED%84%B0%EB%B2%A0%EC%9D%B4%EC%8A%A4-%EC%8A%A4%ED%82%A4%EB%A7%88)
9.  [개발 진행 상황](https://www.google.com/search?q=%23-%EA%B0%9C%EB%B0%9C-%EC%A7%84%ED%96%89-%EC%83%81%ED%99%A9)

<br>

## 📌 프로젝트 소개

**ShieldHub**는 민감한 파일을 안전하게 보호하고 관리하기 위한 백엔드 시스템입니다. AES-256-GCM 이중 암호화 시스템을 통해 강력한 파일 보안을 제공하며, JWT와 OTP/2FA를 결합한 다중 인증 체계로 사용자 계정을 안전하게 보호합니다.

### ✨ 핵심 특징

* **강력한 파일 암호화**: 파일별 고유 키와 마스터 키를 사용하는 **AES-256-GCM 이중 암호화** 시스템
* **다중 인증 시스템**: **JWT** 토큰 인증과 **OTP/2FA**를 결합하여 높은 수준의 계정 보안 제공
* **무결성 보장**: **SHA-256** 해시 검증을 통해 파일의 위변조 여부를 확인
* **사용자별 격리**: 모든 파일과 관련 데이터는 사용자별로 완벽히 격리되어 관리

-----

## 🚀 주요 기능

### 🔐 보안 인증 시스템

* ✅ JWT 기반 토큰 인증 (Access/Refresh)
* ✅ BCrypt를 사용한 비밀번호 단방향 암호화
* ✅ Google Authenticator 연동 OTP/2FA 2단계 인증
* ✅ 회원가입, 로그인, 로그아웃 기능
* ✅ 안전한 비밀번호 변경 및 재설정
* ✅ 이메일 기반 임시 비밀번호 발급

### 📁 파일 보안 시스템

* ✅ **AES-256-GCM**을 사용한 강력한 파일 암호화/복호화
* ✅ 파일 키 + 마스터 키를 이용한 **이중 암호화**로 키 관리 보안 강화
* ✅ **SHA-256** 해시를 이용한 파일 무결성 검증
* ✅ 암호화된 파일과 키, 정보를 하나의 **ZIP**으로 패키징
* ✅ 파일 암호화/복호화 이력 관리
* ✅ 사용자별 파일 저장소 완전 격리

-----

## 🛠️ 기술 스택

| 구분                  | 기술                                                                                              |
| --------------------- | ------------------------------------------------------------------------------------------------- |
| **Backend Framework** | `Java 17`, `Spring Boot 3.2.x`, `Spring Security 6.x`, `Spring Data JPA`, `jjwt 0.11.5`         |
| **Database & Cache** | `MySQL 8.0`, `Redis 7` (세션 관리 예정)                                                             |
| **Security** | `AES-256-GCM`, `SHA-256`, `BCrypt`, `Google Authenticator`                                        |
| **DevOps** | `Docker & Docker Compose`, `Maven`                                                                |

-----

## 🏁 빠른 시작

### 1\. 환경 요구사항

* Java 17 이상
* Docker & Docker Compose
* Maven 3.6 이상

### 2\. 프로젝트 클론

```bash
git clone https://github.com/yourusername/shieldhub-backend.git
cd shieldhub-backend
```

### 3\. 환경 설정

`application-example.yml` 파일을 복사하여 `application.yml` 파일을 생성하고, 주요 설정값을 본인 환경에 맞게 수정합니다.

```bash
cp src/main/resources/application-example.yml src/main/resources/application.yml
```

- **수정 항목:**
   - `app.jwt.secret`: 256비트(32바이트) JWT 시크릿 키
   - `app.encryption.master-key`: 256비트(32바이트) 암호화 마스터 키
   - `spring.datasource.password`: 데이터베이스 비밀번호

### 4\. Docker 컨테이너 실행

`docker-compose.yml` 파일이 있는 프로젝트 루트에서 아래 명령어를 실행하여 데이터베이스 컨테이너를 시작합니다.

```bash
docker-compose up -d
```

### 5\. 애플리케이션 실행

Maven을 사용하여 프로젝트를 빌드하고 실행합니다.

```bash
mvn clean install
mvn spring-boot:run
```

### 6\. 동작 확인

서버가 정상적으로 실행되었는지 확인합니다.

```bash
curl http://localhost:8080/api/test/health
```

"Backend is running\!" 메시지가 출력되면 성공입니다.

-----

## 📑 API 문서

| Method   | Endpoint                          | 설명                      | 인증 필요 |
| :------- | :-------------------------------- | :------------------------ | :-------: |
| **POST** | `/api/auth/register`              | 회원가입                  |     -     |
| **POST** | `/api/auth/login`                 | 일반 로그인               |     -     |
| **POST** | `/api/auth/login-with-otp`        | OTP 로그인                |     -     |
| **POST** | `/api/auth/check-otp`             | OTP 활성화 확인           |     -     |
| **POST** | `/api/auth/logout`                | 로그아웃                  |     ✅     |
| **PUT** | `/api/auth/change-password`       | 비밀번호 변경             |     ✅     |
| **POST** | `/api/auth/reset-password`        | 비밀번호 재설정 요청      |     -     |
| **DELETE**| `/api/auth/delete-account`      | 회원 탈퇴                 |     ✅     |
| **POST** | `/api/otp/setup`                  | OTP 설정용 QR 코드 생성     |     ✅     |
| **POST** | `/api/otp/enable`                 | OTP 활성화                |     ✅     |
| **POST** | `/api/otp/disable`                | OTP 비활성화              |     ✅     |
| **GET** | `/api/otp/status`                 | OTP 상태 확인             |     ✅     |
| **POST** | `/api/files/encrypt`              | 파일 암호화               |     ✅     |
| **POST** | `/api/files/decrypt-upload`       | 파일 복호화 (업로드 방식) |     ✅     |
| **POST** | `/api/files/decrypt/{fileId}`     | 파일 복호화 (DB 참조)     |     ✅     |
| **GET** | `/api/files/list`                 | 내 파일 목록 조회         |     ✅     |

-----

## 📂 프로젝트 구조

```
shieldhub-backend/
│
├── src/main/java/com/shieldhub/backend/
│   ├── config/              # Spring Security, JWT 필터 등 설정
│   ├── controller/          # REST API 엔드포인트
│   ├── dto/                 # 요청(Request) / 응답(Response) 객체
│   ├── entity/              # JPA 엔티티 (데이터베이스 테이블 매핑)
│   ├── repository/          # 데이터 접근 계층 (JPA Repository)
│   ├── service/             # 핵심 비즈니스 로직
│   └── util/                # 암호화, JWT 등 유틸리티 클래스
│
├── src/main/resources/
│   └── application.yml      # Spring Boot 애플리케이션 설정
│
├── docker-compose.yml       # Docker 컨테이너 설정 (MySQL, Redis)
├── sql/init.sql             # 데이터베이스 초기화 스크립트
└── data/                    # (로컬) 암호화 파일 저장소
```

-----

## 🔐 보안 기능

### 🗄️ 파일 암호화 프로세스

1.  사용자가 파일을 업로드하면 서버에서 바이트 배열로 변환합니다.
2.  원본 파일의 **SHA-256 해시값**을 생성하여 무결성 검증용으로 기록합니다.
3.  암호학적으로 안전한 \*\*랜덤 AES-256 키(파일 키)\*\*를 생성합니다.
4.  **AES-GCM 모드**로 파일을 암호화합니다. (안전한 IV 자동 포함)
5.  암호화된 파일은 UUID 기반의 랜덤 파일명으로 서버에 저장됩니다.
6.  파일 암호화에 사용된 **파일 키**를 미리 정의된 **마스터 키**로 한번 더 암호화합니다.
7.  `암호화된 파일`, `암호화된 키`, `파일 정보`를 하나의 **ZIP 파일**로 패키징하여 사용자에게 전달합니다.
8.  관련 메타데이터(파일명, 해시값 등)를 DB에 저장하고 이력을 기록합니다.

### 👤 인증 프로세스

1.  사용자가 ID/PW로 로그인을 요청합니다.
2.  서버는 **BCrypt**로 해시된 비밀번호를 비교하여 사용자를 검증합니다.
3.  검증 성공 시, 사용자의 정보를 담은 **JWT**를 발급합니다.
4.  클라이언트는 이후 모든 요청의 `Authorization` 헤더에 JWT를 담아 전송합니다.
5.  서버의 **JWT 필터**가 토큰을 자동으로 검증하고 사용자를 인증 처리합니다.

-----

## 🗃️ 데이터베이스 스키마

* **Users**
  ```sql
  - user_id (PK), username (UNIQUE), password_hash, name, email, phone_number, otp_secret, is_otp_enabled, created_at, updated_at
  ```
* **FileMetadata**
  ```sql
  - file_id (PK), file_name, user_id (FK), file_path, file_size, sha256_hash, upload_date
  ```
* **FileHistory**
  ```sql
  - history_id (PK), file_id (FK), action_type (ENCRYPTION/DECRYPTION), timestamp
  ```

-----

## 📈 개발 진행 상황

| 구분         | 기능                                | 상태 |
| :----------- | :---------------------------------- | :--: |
| **인프라** | Docker 환경 구축 (MySQL, Redis)     |  ✅  |
|              | Spring Boot 프로젝트 설정           |  ✅  |
|              | DB 스키마 설계 및 구현              |  ✅  |
| **인증 시스템** | JWT 기반 토큰 인증 (Spring Security) |  ✅  |
|              | 회원가입/로그인/로그아웃            |  ✅  |
|              | 비밀번호 변경/재설정                |  ✅  |
|              | OTP/2FA (Google Authenticator)      |  ✅  |
| **파일 보안** | AES-256-GCM 암호화/복호화           |  ✅  |
|              | SHA-256 무결성 검증                 |  ✅  |
|              | 파일 이력 관리                      |  ✅  |
|              | ZIP 패키징 시스템                   |  ✅  |
| **진행 중** | Flask 머신러닝 API 서버 연동        |  ⏳  |
|              | 웹 취약점 분석 기능                 |  ⏳  |
|              | Redis를 이용한 세션/토큰 관리       |  ⏳  |
| **예정** | 이메일 인증 시스템 완성             |  📅  |
|              | 관리자 대시보드                     |  📅  |
|              | 단위 테스트 코드 작성               |  📅  |

-----

### 🧪 테스트 계정

개발 및 테스트용 초기 계정 정보는 다음과 같습니다.

| Username   | Password | Role      |
| :--------- | :------- | :-------- |
| `admin`    | `admin123` | 관리자    |
| `testuser` | `user123`  | 일반 사용자 |

### ⚠️ 주의사항

* JWT Secret, 암호화 마스터 키 등 민감한 정보는 `.gitignore`에 포함되어 소스 코드에 직접 노출되지 않습니다.
* 로컬 환경에서 실행 시, `application-example.yml` 파일을 복사하여 `application.yml` 파일을 반드시 생성해야 합니다.
* 운영 환경에서는 보안을 위해 환경 변수 또는 외부 설정 서버(Spring Cloud Config 등)를 사용하는 것을 강력히 권장합니다.

-----

### 👨‍💻 개발자

* **표상혁** - Backend Developer & Security Engineer
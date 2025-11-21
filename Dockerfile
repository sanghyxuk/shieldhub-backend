# 1. Build Stage
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# 소스 코드 복사
COPY . .

# [디버깅 1] 파일이 제대로 복사되었는지 확인 (로그에 파일 목록이 찍힘)
RUN echo "=== 파일 목록 확인 ===" && ls -al && echo "===================="

# [디버깅 2] build.gradle이 있는지 확인하고, 상세 에러 로그를 출력하며 빌드
# --stacktrace 옵션이 진짜 에러 원인을 알려줍니다.
RUN gradle clean build -x test --no-daemon --stacktrace

# 2. Run Stage
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
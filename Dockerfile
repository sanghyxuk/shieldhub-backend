# 1. Build Stage
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# 소스 코드 복사
COPY . .

# [핵심 변경] gradlew 대신 설치된 gradle 명령어로 빌드 (테스트 제외)
RUN gradle clean build -x test --no-daemon

# 2. Run Stage
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
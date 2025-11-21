# 1. 빌드 단계 (Gradle & JDK 17)
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
# 테스트를 건너뛰고 빌드 (DB 연결 없이 빌드하기 위함)
RUN ./gradlew clean build -x test

# 2. 실행 단계 (가벼운 JDK 17 이미지)
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# Render는 기본적으로 8080 포트를 찾습니다.
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
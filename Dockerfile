# 1. Build Stage (Maven 이미지 사용)
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# 소스 코드 전체 복사
COPY . .

# [Maven 빌드 명령어]
# pom.xml을 읽어서 빌드하고, 테스트는 건너뜁니다(-DskipTests)
RUN mvn clean package -DskipTests

# 2. Run Stage (실행 환경)
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# [중요] Maven은 결과물이 'target' 폴더에 생깁니다. (Gradle은 build/libs)
COPY --from=build /app/target/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
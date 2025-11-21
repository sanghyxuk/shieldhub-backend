FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .

RUN sed -i 's/\r$//' gradlew

RUN chmod +x gradlew

RUN ./gradlew clean build -x test

FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
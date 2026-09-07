FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle.kts settings.gradle.kts
COPY build.gradle.kts build.gradle.kts
COPY reward-core reward-core
COPY reward-engine reward-engine
COPY reward-events reward-events
COPY reward-reporting reward-reporting
COPY reward-api reward-api

RUN chmod +x gradlew && ./gradlew :reward-api:bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /workspace/reward-api/build/libs/reward-api-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# Build stage
FROM openjdk:25-jdk-slim as builder

WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
COPY services/api-service/build.gradle services/api-service/
COPY services/api-service/src services/api-service/src/

# Build the application
RUN chmod +x ./gradlew && ./gradlew :services:api-service:bootJar --no-daemon

# Runtime stage
FROM openjdk:25-jre-slim

WORKDIR /app

# Create non-root user
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# Copy the built jar
COPY --from=builder /app/services/api-service/build/libs/api-service.jar app.jar

# Change ownership
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/v1/actuator/health || exit 1

ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]

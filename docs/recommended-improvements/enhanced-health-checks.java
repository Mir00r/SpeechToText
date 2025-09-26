package com.speechtotext.api.health;

import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Custom health indicator for transcription service dependency.
 */
@Component
public class TranscriptionServiceHealthIndicator implements HealthIndicator {
    
    private final RestTemplate restTemplate;
    private final String transcriptionServiceUrl;
    
    public TranscriptionServiceHealthIndicator(
            RestTemplate restTemplate,
            @Value("${transcription.service.url}") String transcriptionServiceUrl) {
        this.restTemplate = restTemplate;
        this.transcriptionServiceUrl = transcriptionServiceUrl;
    }
    
    @Override
    public Health health() {
        try {
            // Simple health check endpoint
            ResponseEntity<String> response = restTemplate.getForEntity(
                transcriptionServiceUrl + "/health", 
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return Health.up()
                    .withDetail("service", "transcription-service")
                    .withDetail("url", transcriptionServiceUrl)
                    .withDetail("status", "available")
                    .build();
            } else {
                return Health.down()
                    .withDetail("service", "transcription-service")
                    .withDetail("url", transcriptionServiceUrl)
                    .withDetail("status", "unavailable")
                    .withDetail("httpStatus", response.getStatusCode())
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("service", "transcription-service")
                .withDetail("url", transcriptionServiceUrl)
                .withDetail("status", "error")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}

/**
 * Enhanced application.yml health configuration
 */
/*
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,info
  endpoint:
    health:
      show-details: when-authorized
      show-components: always
      probes:
        enabled: true
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true
    db:
      enabled: true
    diskspace:
      enabled: true
      path: /tmp
      threshold: 1GB
    
  metrics:
    export:
      prometheus:
        enabled: true
    web:
      server:
        request:
          autotime:
            enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5,0.9,0.95,0.99
        
  info:
    build:
      enabled: true
    git:
      enabled: true
      mode: full
    java:
      enabled: true
*/

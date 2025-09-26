package com.speechtotext.api.health;

import com.speechtotext.api.service.CircuitBreakerMonitorService;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator that includes circuit breaker status information.
 */
@Component
public class CircuitBreakerHealthIndicator implements HealthIndicator {
    
    private final CircuitBreakerMonitorService circuitBreakerMonitorService;
    
    public CircuitBreakerHealthIndicator(CircuitBreakerMonitorService circuitBreakerMonitorService) {
        this.circuitBreakerMonitorService = circuitBreakerMonitorService;
    }
    
    @Override
    public Health health() {
        try {
            String transcriptionServiceState = circuitBreakerMonitorService.getTranscriptionServiceCircuitBreakerState();
            CircuitBreakerMonitorService.CircuitBreakerMetrics metrics = 
                circuitBreakerMonitorService.getTranscriptionServiceMetrics();
            
            Health.Builder healthBuilder = Health.up();
            
            // Add circuit breaker information
            healthBuilder
                .withDetail("transcriptionService.circuitBreaker.state", transcriptionServiceState)
                .withDetail("transcriptionService.circuitBreaker.failureRate", String.format("%.2f%%", metrics.failureRate()))
                .withDetail("transcriptionService.circuitBreaker.slowCallRate", String.format("%.2f%%", metrics.slowCallRate()))
                .withDetail("transcriptionService.circuitBreaker.successfulCalls", metrics.successfulCalls())
                .withDetail("transcriptionService.circuitBreaker.failedCalls", metrics.failedCalls())
                .withDetail("transcriptionService.circuitBreaker.slowCalls", metrics.slowCalls())
                .withDetail("transcriptionService.circuitBreaker.notPermittedCalls", metrics.notPermittedCalls());
            
            // If circuit breaker is open, mark health as down
            if ("OPEN".equals(transcriptionServiceState)) {
                healthBuilder.down()
                    .withDetail("reason", "Transcription service circuit breaker is OPEN")
                    .withDetail("recommendation", "Check transcription service health and wait for automatic recovery");
            }
            
            // If there are too many recent failures, mark as degraded
            if (metrics.failureRate() > 25.0f && !"OPEN".equals(transcriptionServiceState)) {
                healthBuilder.status("DEGRADED")
                    .withDetail("reason", String.format("High failure rate: %.2f%%", metrics.failureRate()))
                    .withDetail("recommendation", "Monitor transcription service - may open circuit breaker soon");
            }
            
            return healthBuilder.build();
            
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", "Failed to check circuit breaker status")
                .withDetail("exception", e.getMessage())
                .build();
        }
    }
}

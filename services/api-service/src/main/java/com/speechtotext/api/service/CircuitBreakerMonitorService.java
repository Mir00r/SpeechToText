package com.speechtotext.api.service;

import com.speechtotext.api.client.TranscriptionServiceClient;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service to monitor circuit breaker status and provide health information.
 */
@Service
public class CircuitBreakerMonitorService {
    
    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerMonitorService.class);
    
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final TranscriptionServiceClient transcriptionServiceClient;
    
    public CircuitBreakerMonitorService(CircuitBreakerRegistry circuitBreakerRegistry,
                                       TranscriptionServiceClient transcriptionServiceClient) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.transcriptionServiceClient = transcriptionServiceClient;
    }
    
    /**
     * Get the current state of the transcription service circuit breaker.
     */
    public String getTranscriptionServiceCircuitBreakerState() {
        try {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("transcriptionService");
            return circuitBreaker.getState().name();
        } catch (Exception e) {
            logger.warn("Failed to get circuit breaker state", e);
            return "UNKNOWN";
        }
    }
    
    /**
     * Get circuit breaker metrics for transcription service.
     */
    public CircuitBreakerMetrics getTranscriptionServiceMetrics() {
        try {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("transcriptionService");
            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
            
            return new CircuitBreakerMetrics(
                circuitBreaker.getState().name(),
                metrics.getFailureRate(),
                metrics.getSlowCallRate(),
                metrics.getNumberOfSuccessfulCalls(),
                metrics.getNumberOfFailedCalls(),
                metrics.getNumberOfSlowCalls(),
                metrics.getNumberOfNotPermittedCalls()
            );
        } catch (Exception e) {
            logger.warn("Failed to get circuit breaker metrics", e);
            return new CircuitBreakerMetrics("UNKNOWN", 0f, 0f, 0, 0, 0, 0);
        }
    }
    
    /**
     * Check if the transcription service is available through circuit breaker.
     */
    public boolean isTranscriptionServiceAvailable() {
        String state = getTranscriptionServiceCircuitBreakerState();
        return !"OPEN".equals(state);
    }
    
    /**
     * Force transition to half-open state for manual recovery testing.
     * Use with caution - only for testing purposes.
     */
    public void transitionToHalfOpenState() {
        try {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("transcriptionService");
            circuitBreaker.transitionToHalfOpenState();
            logger.info("Circuit breaker transitioned to HALF_OPEN state");
        } catch (Exception e) {
            logger.error("Failed to transition circuit breaker to half-open state", e);
        }
    }
    
    /**
     * Reset circuit breaker to closed state.
     * Use with caution - only for testing purposes.
     */
    public void resetCircuitBreaker() {
        try {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("transcriptionService");
            circuitBreaker.reset();
            logger.info("Circuit breaker reset to CLOSED state");
        } catch (Exception e) {
            logger.error("Failed to reset circuit breaker", e);
        }
    }
    
    /**
     * Data class for circuit breaker metrics.
     */
    public record CircuitBreakerMetrics(
        String state,
        float failureRate,
        float slowCallRate,
        int successfulCalls,
        int failedCalls,
        int slowCalls,
        int notPermittedCalls
    ) {}
}

package com.speechtotext.api.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for circuit breaker and resilience patterns.
 * Provides fault tolerance for external service calls.
 */
@Configuration
public class ResilienceConfig {
    
    /**
     * Circuit breaker configuration for transcription service.
     * - Failure rate threshold: 50%
     * - Wait duration in open state: 30 seconds
     * - Sliding window size: 10 requests
     * - Minimum number of calls: 5
     */
    @Bean
    public CircuitBreaker transcriptionServiceCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50.0f)                    // 50% failure rate to open
            .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s before trying again
            .slidingWindowSize(10)                          // Consider last 10 requests
            .minimumNumberOfCalls(5)                        // Minimum 5 calls before calculating failure rate
            .permittedNumberOfCallsInHalfOpenState(3)       // Allow 3 calls in half-open state
            .slowCallRateThreshold(50.0f)                   // 50% slow calls to open
            .slowCallDurationThreshold(Duration.ofSeconds(10)) // Calls > 10s are slow
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .build();
            
        return CircuitBreaker.of("transcriptionService", config);
    }
    
    /**
     * Time limiter configuration for transcription service.
     * Sets timeout for synchronous calls to 2 minutes.
     */
    @Bean 
    public TimeLimiter transcriptionServiceTimeLimiter() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofMinutes(2))          // 2 minute timeout for sync calls
            .cancelRunningFuture(true)                       // Cancel if timeout
            .build();
            
        return TimeLimiter.of("transcriptionService", config);
    }
    
    /**
     * Circuit breaker configuration for storage operations (S3/MinIO).
     */
    @Bean
    public CircuitBreaker storageCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(60.0f)                    // 60% failure rate (more tolerant)
            .waitDurationInOpenState(Duration.ofSeconds(15)) // Wait 15s for storage
            .slidingWindowSize(8)                           // Consider last 8 requests
            .minimumNumberOfCalls(4)                        // Minimum 4 calls
            .permittedNumberOfCallsInHalfOpenState(2)       // Allow 2 calls in half-open
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .build();
            
        return CircuitBreaker.of("storageService", config);
    }
    
    /**
     * Time limiter for storage operations.
     */
    @Bean
    public TimeLimiter storageTimeLimiter() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(30))         // 30 second timeout for storage
            .cancelRunningFuture(true)
            .build();
            
        return TimeLimiter.of("storageService", config);
    }
}

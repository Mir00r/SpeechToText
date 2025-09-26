package com.speechtotext.api.controller;

import com.speechtotext.api.service.CircuitBreakerMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing circuit breaker state and monitoring.
 * Provides endpoints for monitoring and manually controlling circuit breakers.
 */
@RestController
@RequestMapping("/internal/v1/circuit-breaker")
@Tag(name = "Circuit Breaker Management", description = "Circuit breaker monitoring and control operations")
public class CircuitBreakerController {
    
    private final CircuitBreakerMonitorService circuitBreakerMonitorService;
    
    public CircuitBreakerController(CircuitBreakerMonitorService circuitBreakerMonitorService) {
        this.circuitBreakerMonitorService = circuitBreakerMonitorService;
    }
    
    @GetMapping("/transcription-service/status")
    @Operation(summary = "Get transcription service circuit breaker status")
    public ResponseEntity<String> getTranscriptionServiceStatus() {
        String state = circuitBreakerMonitorService.getTranscriptionServiceCircuitBreakerState();
        return ResponseEntity.ok(state);
    }
    
    @GetMapping("/transcription-service/metrics")
    @Operation(summary = "Get transcription service circuit breaker metrics")
    public ResponseEntity<CircuitBreakerMonitorService.CircuitBreakerMetrics> getTranscriptionServiceMetrics() {
        CircuitBreakerMonitorService.CircuitBreakerMetrics metrics = 
            circuitBreakerMonitorService.getTranscriptionServiceMetrics();
        return ResponseEntity.ok(metrics);
    }
    
    @GetMapping("/transcription-service/available")
    @Operation(summary = "Check if transcription service is available")
    public ResponseEntity<Boolean> isTranscriptionServiceAvailable() {
        boolean available = circuitBreakerMonitorService.isTranscriptionServiceAvailable();
        return ResponseEntity.ok(available);
    }
    
    @PostMapping("/transcription-service/half-open")
    @Operation(summary = "Force transition to half-open state (testing only)")
    public ResponseEntity<String> transitionToHalfOpen() {
        circuitBreakerMonitorService.transitionToHalfOpenState();
        return ResponseEntity.ok("Circuit breaker transitioned to HALF_OPEN state");
    }
    
    @PostMapping("/transcription-service/reset")
    @Operation(summary = "Reset circuit breaker to closed state (testing only)")
    public ResponseEntity<String> resetCircuitBreaker() {
        circuitBreakerMonitorService.resetCircuitBreaker();
        return ResponseEntity.ok("Circuit breaker reset to CLOSED state");
    }
}

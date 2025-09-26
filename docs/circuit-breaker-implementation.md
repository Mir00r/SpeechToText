# Circuit Breaker Implementation for Speech to Text Service

## Overview

This implementation adds comprehensive circuit breaker protection to the Speech to Text service using Resilience4j. Circuit breakers provide fault tolerance by preventing cascading failures and implementing graceful degradation when external services become unavailable.

## Implementation Components

### 1. Dependencies
- `resilience4j-spring-boot3:2.1.0` - Spring Boot 3 integration
- `resilience4j-circuitbreaker:2.1.0` - Core circuit breaker functionality
- `resilience4j-timelimiter:2.1.0` - Time limiting for async operations
- `spring-boot-starter-aop` - AOP support for annotations

### 2. Configuration

#### Application Properties (`application.yml`)
```yaml
resilience4j:
  circuitbreaker:
    instances:
      transcriptionService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        permittedNumberOfCallsInHalfOpenState: 3
        minimumNumberOfCalls: 5
        waitDurationInOpenState: 30s
        failureRateThreshold: 50
        slowCallRateThreshold: 50
        slowCallDurationThreshold: 10s
      storageService:
        registerHealthIndicator: true
        slidingWindowSize: 8
        permittedNumberOfCallsInHalfOpenState: 2
        minimumNumberOfCalls: 4
        waitDurationInOpenState: 15s
        failureRateThreshold: 60

  timelimiter:
    instances:
      transcriptionService:
        timeoutDuration: 2m
        cancelRunningFuture: true
      storageService:
        timeoutDuration: 30s
        cancelRunningFuture: true

  retry:
    instances:
      transcriptionService:
        maxAttempts: 3
        waitDuration: 2s
        exponentialBackoffMultiplier: 2
      storageService:
        maxAttempts: 2
        waitDuration: 1s
```

## Circuit Breaker States

### CLOSED (Normal Operation)
- All requests are allowed through
- Failures and response times are monitored
- Transitions to OPEN when failure/slow call threshold is exceeded

### OPEN (Fault State)  
- All requests are immediately rejected
- Fallback methods are invoked
- Automatically transitions to HALF_OPEN after wait duration

### HALF_OPEN (Recovery Testing)
- Limited number of test requests are allowed
- Transitions back to CLOSED if requests succeed
- Transitions back to OPEN if requests fail

## Protected Services

### 1. Transcription Service Client

#### Methods with Circuit Breaker Protection:
- `submitTranscriptionJob()` - Async transcription submission
- `submitTranscriptionJobSyncAsync()` - Sync transcription with timeout
- `isTranscriptionServiceHealthy()` - Health check

#### Fallback Strategies:
- **Async Submission**: Mark job as failed, return service unavailable error
- **Sync Submission**: Fall back to async processing, return placeholder response
- **Health Check**: Return false (unhealthy)

#### Configuration:
- **Failure Rate Threshold**: 50%
- **Slow Call Threshold**: 10 seconds
- **Wait Duration**: 30 seconds
- **Sliding Window**: 10 requests

### 2. Storage Service (S3/MinIO)

#### Methods with Circuit Breaker Protection:
- `uploadFile()` - File upload operations
- `generatePresignedUrl()` - URL generation

#### Fallback Strategies:
- **File Upload**: Return storage connection exception
- **URL Generation**: Return storage connection exception

#### Configuration:
- **Failure Rate Threshold**: 60%
- **Wait Duration**: 15 seconds
- **Sliding Window**: 8 requests

## Monitoring and Management

### Health Indicators

#### Circuit Breaker Health Indicator
Provides health status including:
- Circuit breaker state (OPEN/CLOSED/HALF_OPEN)
- Failure rate percentage
- Slow call rate percentage
- Success/failure/slow call counts
- Not permitted calls (rejected by open circuit)

#### Health Status Mapping:
- **UP**: Circuit breaker is CLOSED with low failure rate
- **DEGRADED**: High failure rate (>25%) but circuit still closed
- **DOWN**: Circuit breaker is OPEN

### Management Endpoints

#### Circuit Breaker Controller (`/internal/v1/circuit-breaker/`)
- `GET /transcription-service/status` - Get circuit breaker state
- `GET /transcription-service/metrics` - Get detailed metrics
- `GET /transcription-service/available` - Check availability
- `POST /transcription-service/half-open` - Force half-open state (testing)
- `POST /transcription-service/reset` - Reset to closed state (testing)

### Event Logging

Circuit breaker state changes are automatically logged:
- State transitions (CLOSED → OPEN → HALF_OPEN)
- Failure rate exceeded events
- Slow call rate exceeded events
- Call rejection events

## Usage Examples

### Service Method with Circuit Breaker
```java
@Service
public class TranscriptionService {
    
    @CircuitBreaker(name = "transcriptionService", fallbackMethod = "fallbackMethod")
    @Retry(name = "transcriptionService")
    public void processTranscription(UUID jobId, String s3Url) {
        // Call external transcription service
        transcriptionServiceClient.submitTranscriptionJob(jobId, s3Url, false, true);
    }
    
    public void fallbackMethod(UUID jobId, String s3Url, Exception ex) {
        // Fallback logic: queue for later processing, send notification, etc.
        logger.warn("Transcription service unavailable for job {}, queuing for retry", jobId);
        throw new ExternalServiceException.ServiceUnavailableException("transcription-service");
    }
}
```

### Monitoring Circuit Breaker State
```java
@Service
public class MonitoringService {
    
    private final CircuitBreakerMonitorService circuitBreakerMonitor;
    
    public void checkServiceHealth() {
        String state = circuitBreakerMonitor.getTranscriptionServiceCircuitBreakerState();
        
        if ("OPEN".equals(state)) {
            alertingService.sendAlert("Transcription service circuit breaker is OPEN");
        }
        
        CircuitBreakerMetrics metrics = circuitBreakerMonitor.getTranscriptionServiceMetrics();
        metricsCollector.recordFailureRate(metrics.failureRate());
    }
}
```

## Benefits

### 1. Fault Tolerance
- **Cascading Failure Prevention**: Stops failures from propagating through the system
- **Fast Failure**: Immediate rejection during outages reduces resource consumption
- **Automatic Recovery**: Self-healing behavior when services recover

### 2. Improved User Experience
- **Graceful Degradation**: Fallback responses instead of timeouts
- **Reduced Latency**: Fast failure instead of waiting for timeouts
- **Consistent Response Times**: Predictable behavior during service issues

### 3. Resource Protection
- **Thread Pool Protection**: Prevents thread exhaustion during outages
- **Memory Management**: Reduces memory consumption from pending requests
- **Connection Pool Management**: Protects database and HTTP connections

### 4. Operational Benefits
- **Clear Service Status**: Easy to understand service health
- **Automated Recovery**: No manual intervention required
- **Detailed Metrics**: Comprehensive monitoring and alerting capabilities

## Testing

### Unit Tests
- Circuit breaker annotation behavior
- Fallback method execution
- Health check functionality
- Metrics collection

### Integration Tests
```java
@Test
void testCircuitBreakerOpensOnFailures() {
    // Simulate multiple failures
    for (int i = 0; i < 10; i++) {
        assertThrows(ServiceException.class, () -> 
            transcriptionService.processTranscription(UUID.randomUUID(), "s3://test"));
    }
    
    // Verify circuit breaker is now open
    assertEquals("OPEN", circuitBreakerMonitor.getTranscriptionServiceCircuitBreakerState());
    
    // Verify fallback is called immediately
    assertThrows(ServiceUnavailableException.class, () -> 
        transcriptionService.processTranscription(UUID.randomUUID(), "s3://test"));
}
```

## Monitoring and Alerting

### Key Metrics to Monitor
- Circuit breaker state changes
- Failure rate trends
- Response time percentiles
- Call rejection counts

### Recommended Alerts
- Circuit breaker transitions to OPEN state
- Failure rate exceeds 25% for extended periods
- High number of rejected calls
- Circuit breaker stuck in HALF_OPEN state

### Dashboards
Include circuit breaker metrics in monitoring dashboards:
- Service availability percentage
- Circuit breaker state timeline
- Failure rate trends
- Response time impact during outages

## Configuration Tuning

### Failure Rate Threshold
- **Conservative (30-40%)**: More sensitive to failures, quicker protection
- **Moderate (50-60%)**: Balanced approach, good for most services
- **Liberal (70-80%)**: Less sensitive, allows for temporary issues

### Wait Duration
- **Short (10-15s)**: Quick recovery attempts, good for transient issues
- **Medium (30-60s)**: Balanced approach for most scenarios
- **Long (2-5m)**: For services with longer recovery times

### Sliding Window Size
- **Small (5-8)**: Quick response to failure patterns
- **Medium (10-15)**: Balanced statistical significance
- **Large (20-30)**: More stable but slower to react

## Best Practices

1. **Define Clear Fallback Strategies**: Always provide meaningful fallback behavior
2. **Monitor Circuit Breaker Health**: Include in health checks and monitoring
3. **Test Fallback Behavior**: Regularly test circuit breaker and fallback logic
4. **Tune Configuration**: Adjust thresholds based on service characteristics
5. **Document Behavior**: Clear documentation of fallback behavior for operations teams
6. **Gradual Rollout**: Test circuit breaker configuration in staging first
7. **Event Logging**: Ensure all state changes are properly logged for debugging

## Conclusion

The circuit breaker implementation provides robust fault tolerance for the Speech to Text service, ensuring reliable operation even when external services experience issues. The combination of automatic failure detection, graceful degradation, and self-healing behavior significantly improves system resilience and user experience.

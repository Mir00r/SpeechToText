package com.speechtotext.api.events.factory;

import com.speechtotext.api.events.job.JobCreatedEvent;
import com.speechtotext.api.events.job.JobStatusChangedEvent;
import com.speechtotext.api.events.job.JobCompletedEvent;
import com.speechtotext.api.events.api.ApiRequestReceivedEvent;
import com.speechtotext.api.events.storage.FileUploadedEvent;
import com.speechtotext.api.events.system.CircuitBreakerStateChangedEvent;
import com.speechtotext.api.model.JobEntity;
import com.speechtotext.api.trace.TraceContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Factory for creating domain events with proper tracing context.
 * 
 * This factory ensures that all events are created with consistent
 * metadata and tracing information from the current context.
 */
@Component
public class EventFactory {
    
    /**
     * Create a job created event with current tracing context.
     */
    public JobCreatedEvent createJobCreatedEvent(String jobId, String filename, 
                                                String originalFilename, String storageUrl,
                                                String model, String language, String quality,
                                                Long fileSizeBytes, BigDecimal estimatedDurationSeconds,
                                                Boolean enableDiarization, Boolean syncMode,
                                                String clientIp, String userAgent) {
        return (JobCreatedEvent) new JobCreatedEvent.Builder()
                .aggregateId(jobId)
                .correlationId(TraceContext.getCorrelationId())
                .initiatedBy(getInitiator())
                .filename(filename)
                .originalFilename(originalFilename)
                .storageUrl(storageUrl)
                .model(model)
                .language(language)
                .quality(quality)
                .fileSizeBytes(fileSizeBytes)
                .estimatedDurationSeconds(estimatedDurationSeconds)
                .enableDiarization(enableDiarization)
                .syncMode(syncMode)
                .clientIp(clientIp)
                .userAgent(userAgent)
                .metadata("requestId", TraceContext.getRequestId())
                .metadata("traceId", TraceContext.getTraceId())
                .build();
    }
    
    /**
     * Create a job status changed event with current tracing context.
     */
    public JobStatusChangedEvent createJobStatusChangedEvent(String jobId, 
                                                           JobEntity.JobStatus previousStatus,
                                                           JobEntity.JobStatus newStatus,
                                                           String reason, String errorMessage,
                                                           Long processingTimeMs) {
        return (JobStatusChangedEvent) new JobStatusChangedEvent.Builder()
                .aggregateId(jobId)
                .correlationId(TraceContext.getCorrelationId())
                .initiatedBy(getInitiator())
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .reason(reason)
                .errorMessage(errorMessage)
                .processingTimeMs(processingTimeMs)
                .metadata("requestId", TraceContext.getRequestId())
                .metadata("traceId", TraceContext.getTraceId())
                .build();
    }
    
    /**
     * Create a job completed event with current tracing context.
     */
    public JobCompletedEvent createJobCompletedEvent(String jobId, String transcriptText,
                                                    BigDecimal confidence, String language,
                                                    String modelUsed, BigDecimal processingTimeSeconds,
                                                    String transcriptUrl, String timestampsUrl,
                                                    Integer segmentCount, Integer wordCount,
                                                    Integer speakerCount, Boolean diarizationEnabled) {
        return (JobCompletedEvent) new JobCompletedEvent.Builder()
                .aggregateId(jobId)
                .correlationId(TraceContext.getCorrelationId())
                .initiatedBy(getInitiator())
                .transcriptText(transcriptText)
                .confidence(confidence)
                .language(language)
                .modelUsed(modelUsed)
                .processingTimeSeconds(processingTimeSeconds)
                .transcriptUrl(transcriptUrl)
                .timestampsUrl(timestampsUrl)
                .segmentCount(segmentCount)
                .wordCount(wordCount)
                .speakerCount(speakerCount)
                .diarizationEnabled(diarizationEnabled)
                .metadata("requestId", TraceContext.getRequestId())
                .metadata("traceId", TraceContext.getTraceId())
                .build();
    }
    
    /**
     * Create an API request received event with current tracing context.
     */
    public ApiRequestReceivedEvent createApiRequestReceivedEvent(String requestId, String method,
                                                               String path, String clientIp,
                                                               String userAgent, String contentType,
                                                               Long contentLength, String authenticationInfo,
                                                               Map<String, String> queryParams) {
        return (ApiRequestReceivedEvent) new ApiRequestReceivedEvent.Builder()
                .aggregateId(requestId)
                .correlationId(TraceContext.getCorrelationId())
                .initiatedBy(clientIp)
                .requestId(requestId)
                .method(method)
                .path(path)
                .clientIp(clientIp)
                .userAgent(userAgent)
                .contentType(contentType)
                .contentLength(contentLength)
                .authenticationInfo(authenticationInfo)
                .queryParams(queryParams)
                .metadata("traceId", TraceContext.getTraceId())
                .build();
    }
    
    /**
     * Create a file uploaded event with current tracing context.
     */
    public FileUploadedEvent createFileUploadedEvent(String filename, String originalFilename,
                                                   String storageUrl, String bucket,
                                                   Long fileSizeBytes, String contentType,
                                                   String checksumMd5, Long uploadTimeMs,
                                                   String clientIp) {
        return (FileUploadedEvent) new FileUploadedEvent.Builder()
                .aggregateId(filename)
                .correlationId(TraceContext.getCorrelationId())
                .initiatedBy(getInitiator())
                .filename(filename)
                .originalFilename(originalFilename)
                .storageUrl(storageUrl)
                .bucket(bucket)
                .fileSizeBytes(fileSizeBytes)
                .contentType(contentType)
                .checksumMd5(checksumMd5)
                .uploadTimeMs(uploadTimeMs)
                .clientIp(clientIp)
                .metadata("requestId", TraceContext.getRequestId())
                .metadata("traceId", TraceContext.getTraceId())
                .build();
    }
    
    /**
     * Create a circuit breaker state changed event with current tracing context.
     */
    public CircuitBreakerStateChangedEvent createCircuitBreakerStateChangedEvent(
            String circuitBreakerName, String previousState, String newState,
            String reason, Float failureRate, Float slowCallRate,
            Integer numberOfCalls, Integer numberOfFailedCalls, String serviceName) {
        return (CircuitBreakerStateChangedEvent) new CircuitBreakerStateChangedEvent.Builder()
                .aggregateId(circuitBreakerName)
                .correlationId(TraceContext.getCorrelationId())
                .initiatedBy("system")
                .circuitBreakerName(circuitBreakerName)
                .previousState(previousState)
                .newState(newState)
                .reason(reason)
                .failureRate(failureRate)
                .slowCallRate(slowCallRate)
                .numberOfCalls(numberOfCalls)
                .numberOfFailedCalls(numberOfFailedCalls)
                .serviceName(serviceName)
                .metadata("requestId", TraceContext.getRequestId())
                .metadata("traceId", TraceContext.getTraceId())
                .build();
    }
    
    /**
     * Get the current initiator from context or default to "system".
     */
    private String getInitiator() {
        // Could be enhanced to get actual user information from security context
        return "system"; // Simplified for now, could extract from request context
    }
}

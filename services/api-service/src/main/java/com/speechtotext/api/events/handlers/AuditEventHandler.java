package com.speechtotext.api.events.handlers;

import com.speechtotext.api.events.job.JobCreatedEvent;
import com.speechtotext.api.events.job.JobStatusChangedEvent;
import com.speechtotext.api.events.job.JobCompletedEvent;
import com.speechtotext.api.events.api.ApiRequestReceivedEvent;
import com.speechtotext.api.events.storage.FileUploadedEvent;
import com.speechtotext.api.events.system.CircuitBreakerStateChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event handler for processing domain events asynchronously.
 * 
 * This handler provides logging, metrics collection, and other
 * cross-cutting concerns for domain events.
 */
@Component
public class AuditEventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditEventHandler.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
    
    @EventListener
    @Async
    public void handleJobCreatedEvent(JobCreatedEvent event) {
        var payload = (JobCreatedEvent.JobCreatedPayload) event.getPayload();
        auditLogger.info("JOB_CREATED: {} - File: {} ({} bytes) [correlationId={}]",
                event.getAggregateId(),
                payload.getOriginalFilename(),
                payload.getFileSizeBytes(),
                event.getCorrelationId());
                
        logger.debug("Processing JobCreatedEvent for aggregate: {}", event.getAggregateId());
    }
    
    @EventListener
    @Async
    public void handleJobStatusChangedEvent(JobStatusChangedEvent event) {
        var payload = (JobStatusChangedEvent.JobStatusChangedPayload) event.getPayload();
        auditLogger.info("JOB_STATUS_CHANGED: {} - {} -> {} [correlationId={}]",
                event.getAggregateId(),
                payload.getPreviousStatus(),
                payload.getNewStatus(),
                event.getCorrelationId());
                
        // Log additional details for failed jobs
        if (payload.getNewStatus() == 
            com.speechtotext.api.model.JobEntity.JobStatus.FAILED) {
            auditLogger.warn("JOB_FAILED: {} - Reason: {} [correlationId={}]",
                    event.getAggregateId(),
                    payload.getErrorMessage(),
                    event.getCorrelationId());
        }
        
        logger.debug("Processing JobStatusChangedEvent for aggregate: {}", event.getAggregateId());
    }
    
    @EventListener
    @Async
    public void handleJobCompletedEvent(JobCompletedEvent event) {
        var payload = (JobCompletedEvent.JobCompletedPayload) event.getPayload();
        auditLogger.info("JOB_COMPLETED: {} - Language: {}, Model: {}, Processing Time: {}s [correlationId={}]",
                event.getAggregateId(),
                payload.getLanguage(),
                payload.getModelUsed(),
                payload.getProcessingTimeSeconds(),
                event.getCorrelationId());
                
        logger.debug("Processing JobCompletedEvent for aggregate: {}", event.getAggregateId());
    }
    
    @EventListener
    @Async
    public void handleApiRequestReceivedEvent(ApiRequestReceivedEvent event) {
        var payload = (ApiRequestReceivedEvent.ApiRequestReceivedPayload) event.getPayload();
        auditLogger.info("API_REQUEST: {} {} - Client: {} [correlationId={}]",
                payload.getMethod(),
                payload.getPath(),
                payload.getClientIp(),
                event.getCorrelationId());
                
        logger.debug("Processing ApiRequestReceivedEvent for request: {}", event.getAggregateId());
    }
    
    @EventListener
    @Async
    public void handleFileUploadedEvent(FileUploadedEvent event) {
        var payload = (FileUploadedEvent.FileUploadedPayload) event.getPayload();
        auditLogger.info("FILE_UPLOADED: {} - {} ({} bytes) in {}ms [correlationId={}]",
                payload.getFilename(),
                payload.getOriginalFilename(),
                payload.getFileSizeBytes(),
                payload.getUploadTimeMs(),
                event.getCorrelationId());
                
        logger.debug("Processing FileUploadedEvent for file: {}", payload.getFilename());
    }
    
    @EventListener
    @Async
    public void handleCircuitBreakerStateChangedEvent(CircuitBreakerStateChangedEvent event) {
        var payload = (CircuitBreakerStateChangedEvent.CircuitBreakerStateChangedPayload) event.getPayload();
        auditLogger.warn("CIRCUIT_BREAKER_STATE_CHANGED: {} - {} -> {} (Failure Rate: {}%) [correlationId={}]",
                payload.getCircuitBreakerName(),
                payload.getPreviousState(),
                payload.getNewState(),
                payload.getFailureRate(),
                event.getCorrelationId());
                
        logger.debug("Processing CircuitBreakerStateChangedEvent for: {}", 
                payload.getCircuitBreakerName());
    }
}

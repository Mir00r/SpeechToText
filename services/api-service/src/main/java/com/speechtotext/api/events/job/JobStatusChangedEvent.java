package com.speechtotext.api.events.job;

import com.speechtotext.api.events.BaseDomainEvent;
import com.speechtotext.api.events.DomainEvent;
import com.speechtotext.api.model.JobEntity;

import java.util.Map;

/**
 * Event fired when a transcription job status changes.
 * 
 * This event captures status transitions and provides audit trail
 * for job state changes throughout the processing lifecycle.
 */
public class JobStatusChangedEvent extends BaseDomainEvent {
    
    private final JobStatusChangedPayload payload;
    
    private JobStatusChangedEvent(Builder builder) {
        super(builder);
        this.payload = new JobStatusChangedPayload(
                builder.previousStatus,
                builder.newStatus,
                builder.reason,
                builder.errorMessage,
                builder.processingTimeMs
        );
    }
    
    @Override
    public Object getPayload() {
        return payload;
    }
    
    /**
     * Payload containing status change details.
     */
    public static class JobStatusChangedPayload {
        private final JobEntity.JobStatus previousStatus;
        private final JobEntity.JobStatus newStatus;
        private final String reason;
        private final String errorMessage;
        private final Long processingTimeMs;
        
        public JobStatusChangedPayload(JobEntity.JobStatus previousStatus, 
                                      JobEntity.JobStatus newStatus,
                                      String reason, 
                                      String errorMessage,
                                      Long processingTimeMs) {
            this.previousStatus = previousStatus;
            this.newStatus = newStatus;
            this.reason = reason;
            this.errorMessage = errorMessage;
            this.processingTimeMs = processingTimeMs;
        }
        
        // Getters
        public JobEntity.JobStatus getPreviousStatus() { return previousStatus; }
        public JobEntity.JobStatus getNewStatus() { return newStatus; }
        public String getReason() { return reason; }
        public String getErrorMessage() { return errorMessage; }
        public Long getProcessingTimeMs() { return processingTimeMs; }
    }
    
    /**
     * Builder for creating JobStatusChangedEvent instances.
     */
    public static class Builder extends BaseDomainEvent.Builder {
        private JobEntity.JobStatus previousStatus;
        private JobEntity.JobStatus newStatus;
        private String reason;
        private String errorMessage;
        private Long processingTimeMs;
        
        public Builder() {
            eventType("JobStatusChanged");
            aggregateType("TranscriptionJob");
        }
        
        public Builder previousStatus(JobEntity.JobStatus previousStatus) {
            this.previousStatus = previousStatus;
            return this;
        }
        
        public Builder newStatus(JobEntity.JobStatus newStatus) {
            this.newStatus = newStatus;
            return this;
        }
        
        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public Builder processingTimeMs(Long processingTimeMs) {
            this.processingTimeMs = processingTimeMs;
            return this;
        }
        
        @Override
        public Builder aggregateId(String aggregateId) {
            super.aggregateId(aggregateId);
            return this;
        }
        
        @Override
        public Builder correlationId(String correlationId) {
            super.correlationId(correlationId);
            return this;
        }
        
        @Override
        public Builder initiatedBy(String initiatedBy) {
            super.initiatedBy(initiatedBy);
            return this;
        }
        
        @Override
        public Builder metadata(String key, Object value) {
            super.metadata(key, value);
            return this;
        }
        
        @Override
        public Builder metadata(Map<String, Object> metadata) {
            super.metadata(metadata);
            return this;
        }
        
        @Override
        public Builder sequenceNumber(Long sequenceNumber) {
            super.sequenceNumber(sequenceNumber);
            return this;
        }
        
        @Override
        public DomainEvent build() {
            if (newStatus == null) {
                throw new IllegalArgumentException("newStatus is required");
            }
            return new JobStatusChangedEvent(this);
        }
    }
}

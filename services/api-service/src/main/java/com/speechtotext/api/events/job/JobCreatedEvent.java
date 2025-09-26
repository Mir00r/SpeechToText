package com.speechtotext.api.events.job;

import com.speechtotext.api.events.BaseDomainEvent;
import com.speechtotext.api.events.DomainEvent;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Event fired when a new transcription job is created.
 * 
 * This event captures the initial state of a transcription job including
 * file information, processing parameters, and metadata.
 */
public class JobCreatedEvent extends BaseDomainEvent {
    
    private final JobCreatedPayload payload;
    
    private JobCreatedEvent(Builder builder) {
        super(builder);
        this.payload = new JobCreatedPayload(
                builder.filename,
                builder.originalFilename,
                builder.storageUrl,
                builder.model,
                builder.language,
                builder.quality,
                builder.fileSizeBytes,
                builder.estimatedDurationSeconds,
                builder.enableDiarization,
                builder.syncMode,
                builder.clientIp,
                builder.userAgent
        );
    }
    
    @Override
    public Object getPayload() {
        return payload;
    }
    
    /**
     * Payload containing job creation details.
     */
    public static class JobCreatedPayload {
        private final String filename;
        private final String originalFilename;
        private final String storageUrl;
        private final String model;
        private final String language;
        private final String quality;
        private final Long fileSizeBytes;
        private final BigDecimal estimatedDurationSeconds;
        private final Boolean enableDiarization;
        private final Boolean syncMode;
        private final String clientIp;
        private final String userAgent;
        
        public JobCreatedPayload(String filename, String originalFilename, String storageUrl,
                                String model, String language, String quality, Long fileSizeBytes,
                                BigDecimal estimatedDurationSeconds, Boolean enableDiarization,
                                Boolean syncMode, String clientIp, String userAgent) {
            this.filename = filename;
            this.originalFilename = originalFilename;
            this.storageUrl = storageUrl;
            this.model = model;
            this.language = language;
            this.quality = quality;
            this.fileSizeBytes = fileSizeBytes;
            this.estimatedDurationSeconds = estimatedDurationSeconds;
            this.enableDiarization = enableDiarization;
            this.syncMode = syncMode;
            this.clientIp = clientIp;
            this.userAgent = userAgent;
        }
        
        // Getters
        public String getFilename() { return filename; }
        public String getOriginalFilename() { return originalFilename; }
        public String getStorageUrl() { return storageUrl; }
        public String getModel() { return model; }
        public String getLanguage() { return language; }
        public String getQuality() { return quality; }
        public Long getFileSizeBytes() { return fileSizeBytes; }
        public BigDecimal getEstimatedDurationSeconds() { return estimatedDurationSeconds; }
        public Boolean getEnableDiarization() { return enableDiarization; }
        public Boolean getSyncMode() { return syncMode; }
        public String getClientIp() { return clientIp; }
        public String getUserAgent() { return userAgent; }
    }
    
    /**
     * Builder for creating JobCreatedEvent instances.
     */
    public static class Builder extends BaseDomainEvent.Builder {
        private String filename;
        private String originalFilename;
        private String storageUrl;
        private String model;
        private String language;
        private String quality;
        private Long fileSizeBytes;
        private BigDecimal estimatedDurationSeconds;
        private Boolean enableDiarization;
        private Boolean syncMode;
        private String clientIp;
        private String userAgent;
        
        public Builder() {
            eventType("JobCreated");
            aggregateType("TranscriptionJob");
        }
        
        public Builder filename(String filename) {
            this.filename = filename;
            return this;
        }
        
        public Builder originalFilename(String originalFilename) {
            this.originalFilename = originalFilename;
            return this;
        }
        
        public Builder storageUrl(String storageUrl) {
            this.storageUrl = storageUrl;
            return this;
        }
        
        public Builder model(String model) {
            this.model = model;
            return this;
        }
        
        public Builder language(String language) {
            this.language = language;
            return this;
        }
        
        public Builder quality(String quality) {
            this.quality = quality;
            return this;
        }
        
        public Builder fileSizeBytes(Long fileSizeBytes) {
            this.fileSizeBytes = fileSizeBytes;
            return this;
        }
        
        public Builder estimatedDurationSeconds(BigDecimal estimatedDurationSeconds) {
            this.estimatedDurationSeconds = estimatedDurationSeconds;
            return this;
        }
        
        public Builder enableDiarization(Boolean enableDiarization) {
            this.enableDiarization = enableDiarization;
            return this;
        }
        
        public Builder syncMode(Boolean syncMode) {
            this.syncMode = syncMode;
            return this;
        }
        
        public Builder clientIp(String clientIp) {
            this.clientIp = clientIp;
            return this;
        }
        
        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
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
            if (filename == null || originalFilename == null || storageUrl == null) {
                throw new IllegalArgumentException("filename, originalFilename, and storageUrl are required");
            }
            return new JobCreatedEvent(this);
        }
    }
}

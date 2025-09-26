package com.speechtotext.api.events.storage;

import com.speechtotext.api.events.BaseDomainEvent;
import com.speechtotext.api.events.DomainEvent;

import java.util.Map;

/**
 * Event fired when a file is uploaded to storage.
 * 
 * This event captures file upload operations for audit trail
 * and tracking storage operations.
 */
public class FileUploadedEvent extends BaseDomainEvent {
    
    private final FileUploadedPayload payload;
    
    private FileUploadedEvent(Builder builder) {
        super(builder);
        this.payload = new FileUploadedPayload(
                builder.filename,
                builder.originalFilename,
                builder.storageUrl,
                builder.bucket,
                builder.fileSizeBytes,
                builder.contentType,
                builder.checksumMd5,
                builder.uploadTimeMs,
                builder.clientIp
        );
    }
    
    @Override
    public Object getPayload() {
        return payload;
    }
    
    /**
     * Payload containing file upload details.
     */
    public static class FileUploadedPayload {
        private final String filename;
        private final String originalFilename;
        private final String storageUrl;
        private final String bucket;
        private final Long fileSizeBytes;
        private final String contentType;
        private final String checksumMd5;
        private final Long uploadTimeMs;
        private final String clientIp;
        
        public FileUploadedPayload(String filename, String originalFilename, String storageUrl,
                                  String bucket, Long fileSizeBytes, String contentType,
                                  String checksumMd5, Long uploadTimeMs, String clientIp) {
            this.filename = filename;
            this.originalFilename = originalFilename;
            this.storageUrl = storageUrl;
            this.bucket = bucket;
            this.fileSizeBytes = fileSizeBytes;
            this.contentType = contentType;
            this.checksumMd5 = checksumMd5;
            this.uploadTimeMs = uploadTimeMs;
            this.clientIp = clientIp;
        }
        
        // Getters
        public String getFilename() { return filename; }
        public String getOriginalFilename() { return originalFilename; }
        public String getStorageUrl() { return storageUrl; }
        public String getBucket() { return bucket; }
        public Long getFileSizeBytes() { return fileSizeBytes; }
        public String getContentType() { return contentType; }
        public String getChecksumMd5() { return checksumMd5; }
        public Long getUploadTimeMs() { return uploadTimeMs; }
        public String getClientIp() { return clientIp; }
    }
    
    /**
     * Builder for creating FileUploadedEvent instances.
     */
    public static class Builder extends BaseDomainEvent.Builder {
        private String filename;
        private String originalFilename;
        private String storageUrl;
        private String bucket;
        private Long fileSizeBytes;
        private String contentType;
        private String checksumMd5;
        private Long uploadTimeMs;
        private String clientIp;
        
        public Builder() {
            eventType("FileUploaded");
            aggregateType("FileStorage");
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
        
        public Builder bucket(String bucket) {
            this.bucket = bucket;
            return this;
        }
        
        public Builder fileSizeBytes(Long fileSizeBytes) {
            this.fileSizeBytes = fileSizeBytes;
            return this;
        }
        
        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }
        
        public Builder checksumMd5(String checksumMd5) {
            this.checksumMd5 = checksumMd5;
            return this;
        }
        
        public Builder uploadTimeMs(Long uploadTimeMs) {
            this.uploadTimeMs = uploadTimeMs;
            return this;
        }
        
        public Builder clientIp(String clientIp) {
            this.clientIp = clientIp;
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
            if (filename == null || storageUrl == null) {
                throw new IllegalArgumentException("filename and storageUrl are required");
            }
            return new FileUploadedEvent(this);
        }
    }
}

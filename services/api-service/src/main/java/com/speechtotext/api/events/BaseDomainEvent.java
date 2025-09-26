package com.speechtotext.api.events;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Abstract base implementation of DomainEvent that provides common functionality.
 * 
 * This base class handles the common fields and provides a template for creating
 * specific domain events with proper metadata and tracing information.
 */
public abstract class BaseDomainEvent implements DomainEvent {
    
    private final UUID eventId;
    private final String eventType;
    private final int version;
    private final LocalDateTime occurredAt;
    private final String aggregateId;
    private final String aggregateType;
    private final String correlationId;
    private final String initiatedBy;
    private final Map<String, Object> metadata;
    private final Long sequenceNumber;
    
    protected BaseDomainEvent(Builder builder) {
        this.eventId = builder.eventId != null ? builder.eventId : UUID.randomUUID();
        this.eventType = builder.eventType;
        this.version = builder.version > 0 ? builder.version : 1;
        this.occurredAt = builder.occurredAt != null ? builder.occurredAt : LocalDateTime.now();
        this.aggregateId = builder.aggregateId;
        this.aggregateType = builder.aggregateType;
        this.correlationId = builder.correlationId;
        this.initiatedBy = builder.initiatedBy;
        this.metadata = new HashMap<>(builder.metadata);
        this.sequenceNumber = builder.sequenceNumber;
    }
    
    @Override
    public UUID getEventId() {
        return eventId;
    }
    
    @Override
    public String getEventType() {
        return eventType;
    }
    
    @Override
    public int getVersion() {
        return version;
    }
    
    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
    
    @Override
    public String getAggregateId() {
        return aggregateId;
    }
    
    @Override
    public String getAggregateType() {
        return aggregateType;
    }
    
    @Override
    public String getCorrelationId() {
        return correlationId;
    }
    
    @Override
    public String getInitiatedBy() {
        return initiatedBy;
    }
    
    @Override
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    @Override
    public Long getSequenceNumber() {
        return sequenceNumber;
    }
    
    /**
     * Builder for creating BaseDomainEvent instances.
     */
    public abstract static class Builder {
        protected UUID eventId;
        protected String eventType;
        protected int version = 1;
        protected LocalDateTime occurredAt;
        protected String aggregateId;
        protected String aggregateType;
        protected String correlationId;
        protected String initiatedBy;
        protected Map<String, Object> metadata = new HashMap<>();
        protected Long sequenceNumber;
        
        public Builder eventId(UUID eventId) {
            this.eventId = eventId;
            return this;
        }
        
        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }
        
        public Builder version(int version) {
            this.version = version;
            return this;
        }
        
        public Builder occurredAt(LocalDateTime occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }
        
        public Builder aggregateId(String aggregateId) {
            this.aggregateId = aggregateId;
            return this;
        }
        
        public Builder aggregateType(String aggregateType) {
            this.aggregateType = aggregateType;
            return this;
        }
        
        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }
        
        public Builder initiatedBy(String initiatedBy) {
            this.initiatedBy = initiatedBy;
            return this;
        }
        
        public Builder metadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata.putAll(metadata);
            return this;
        }
        
        public Builder sequenceNumber(Long sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
            return this;
        }
        
        public abstract DomainEvent build();
    }
    
    @Override
    public String toString() {
        return String.format("%s{eventId=%s, eventType='%s', aggregateId='%s', occurredAt=%s, correlationId='%s'}", 
                getClass().getSimpleName(), eventId, eventType, aggregateId, occurredAt, correlationId);
    }
}

package com.speechtotext.api.events.store;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for storing domain events in the event store.
 * 
 * This entity provides persistence for all domain events, enabling
 * audit trails, event replay, and comprehensive system observability.
 */
@Entity
@Table(name = "event_store", indexes = {
    @Index(name = "idx_event_store_aggregate_id", columnList = "aggregate_id"),
    @Index(name = "idx_event_store_aggregate_type", columnList = "aggregate_type"),
    @Index(name = "idx_event_store_event_type", columnList = "event_type"),
    @Index(name = "idx_event_store_correlation_id", columnList = "correlation_id"),
    @Index(name = "idx_event_store_occurred_at", columnList = "occurred_at"),
    @Index(name = "idx_event_store_sequence_number", columnList = "aggregate_id, sequence_number")
})
public class EventStoreEntry {
    
    @Id
    @Column(name = "event_id")
    private UUID eventId;
    
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    
    @Column(name = "event_version", nullable = false)
    private int eventVersion;
    
    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;
    
    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;
    
    @Column(name = "sequence_number")
    private Long sequenceNumber;
    
    @Column(name = "correlation_id", length = 100)
    private String correlationId;
    
    @Column(name = "initiated_by", length = 100)
    private String initiatedBy;
    
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
    
    @CreationTimestamp
    @Column(name = "stored_at", nullable = false, updatable = false)
    private LocalDateTime storedAt;
    
    @Column(name = "event_data", columnDefinition = "jsonb", nullable = false)
    private String eventData;
    
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;
    
    // Default constructor for JPA
    protected EventStoreEntry() {}
    
    public EventStoreEntry(UUID eventId, String eventType, int eventVersion,
                          String aggregateId, String aggregateType, Long sequenceNumber,
                          String correlationId, String initiatedBy, LocalDateTime occurredAt,
                          String eventData, String metadata) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.sequenceNumber = sequenceNumber;
        this.correlationId = correlationId;
        this.initiatedBy = initiatedBy;
        this.occurredAt = occurredAt;
        this.eventData = eventData;
        this.metadata = metadata;
    }
    
    // Getters and Setters
    public UUID getEventId() {
        return eventId;
    }
    
    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public int getEventVersion() {
        return eventVersion;
    }
    
    public void setEventVersion(int eventVersion) {
        this.eventVersion = eventVersion;
    }
    
    public String getAggregateId() {
        return aggregateId;
    }
    
    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }
    
    public String getAggregateType() {
        return aggregateType;
    }
    
    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }
    
    public Long getSequenceNumber() {
        return sequenceNumber;
    }
    
    public void setSequenceNumber(Long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    public String getInitiatedBy() {
        return initiatedBy;
    }
    
    public void setInitiatedBy(String initiatedBy) {
        this.initiatedBy = initiatedBy;
    }
    
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
    
    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
    
    public LocalDateTime getStoredAt() {
        return storedAt;
    }
    
    public void setStoredAt(LocalDateTime storedAt) {
        this.storedAt = storedAt;
    }
    
    public String getEventData() {
        return eventData;
    }
    
    public void setEventData(String eventData) {
        this.eventData = eventData;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public String toString() {
        return String.format("EventStoreEntry{eventId=%s, eventType='%s', aggregateId='%s', occurredAt=%s}", 
                eventId, eventType, aggregateId, occurredAt);
    }
}

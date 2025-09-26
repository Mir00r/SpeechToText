package com.speechtotext.api.events;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Base interface for all domain events in the Speech to Text system.
 * 
 * Events represent things that have happened in the past and are immutable.
 * They capture the state changes and important business events for audit trails,
 * debugging, and potential event replay functionality.
 */
public interface DomainEvent {
    
    /**
     * Unique identifier for this event.
     */
    UUID getEventId();
    
    /**
     * Type/name of the event (e.g., "JobCreated", "TranscriptionCompleted").
     */
    String getEventType();
    
    /**
     * Version of the event schema for backward compatibility.
     */
    int getVersion();
    
    /**
     * Timestamp when the event occurred.
     */
    LocalDateTime getOccurredAt();
    
    /**
     * ID of the aggregate root that this event belongs to.
     */
    String getAggregateId();
    
    /**
     * Type of the aggregate (e.g., "TranscriptionJob", "SystemHealth").
     */
    String getAggregateType();
    
    /**
     * Correlation ID for tracing across service boundaries.
     */
    String getCorrelationId();
    
    /**
     * Optional user/system that triggered this event.
     */
    String getInitiatedBy();
    
    /**
     * Additional metadata for the event.
     */
    Map<String, Object> getMetadata();
    
    /**
     * Event payload containing the specific event data.
     */
    Object getPayload();
    
    /**
     * Sequence number for ordering events within an aggregate.
     */
    Long getSequenceNumber();
}

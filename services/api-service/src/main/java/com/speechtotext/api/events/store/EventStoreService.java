package com.speechtotext.api.events.store;

import com.speechtotext.api.events.DomainEvent;
import com.speechtotext.api.events.publisher.EventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * Service for storing and publishing domain events.
 * 
 * This service provides the core event sourcing functionality by persisting
 * all domain events to the event store and publishing them for processing.
 */
@Service
@Transactional
public class EventStoreService implements EventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(EventStoreService.class);
    
    private final EventStoreRepository eventStoreRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;
    
    public EventStoreService(EventStoreRepository eventStoreRepository,
                            ApplicationEventPublisher applicationEventPublisher,
                            ObjectMapper objectMapper) {
        this.eventStoreRepository = eventStoreRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void publish(DomainEvent event) {
        try {
            // Store the event in the event store
            EventStoreEntry entry = storeEvent(event);
            logger.debug("Stored event: {}", entry);
            
            // Publish the event for immediate processing
            applicationEventPublisher.publishEvent(event);
            
            logger.info("Published event: {} for aggregate: {} [correlationId={}]", 
                       event.getEventType(), event.getAggregateId(), event.getCorrelationId());
                       
        } catch (Exception e) {
            logger.error("Failed to publish event: {} for aggregate: {} [correlationId={}]", 
                        event.getEventType(), event.getAggregateId(), event.getCorrelationId(), e);
            throw new EventStoreException("Failed to publish event", e);
        }
    }
    
    @Override
    public void publishAll(DomainEvent... events) {
        publishAll(Arrays.asList(events));
    }
    
    @Override
    public void publishAll(Iterable<DomainEvent> events) {
        try {
            for (DomainEvent event : events) {
                storeEvent(event);
            }
            
            // Publish all events after successful storage
            for (DomainEvent event : events) {
                applicationEventPublisher.publishEvent(event);
            }
            
            logger.info("Published batch of events successfully");
            
        } catch (Exception e) {
            logger.error("Failed to publish batch of events", e);
            throw new EventStoreException("Failed to publish batch of events", e);
        }
    }
    
    /**
     * Store a single event in the event store.
     */
    private EventStoreEntry storeEvent(DomainEvent event) {
        try {
            // Determine sequence number for this aggregate
            Long sequenceNumber = event.getSequenceNumber();
            if (sequenceNumber == null) {
                sequenceNumber = getNextSequenceNumber(event.getAggregateId());
            }
            
            // Serialize event payload and metadata
            String eventData = serializeEventData(event);
            String metadata = serializeMetadata(event);
            
            // Create and save the event store entry
            EventStoreEntry entry = new EventStoreEntry(
                    event.getEventId(),
                    event.getEventType(),
                    event.getVersion(),
                    event.getAggregateId(),
                    event.getAggregateType(),
                    sequenceNumber,
                    event.getCorrelationId(),
                    event.getInitiatedBy(),
                    event.getOccurredAt(),
                    eventData,
                    metadata
            );
            
            return eventStoreRepository.save(entry);
            
        } catch (Exception e) {
            throw new EventStoreException("Failed to store event in event store", e);
        }
    }
    
    /**
     * Get the next sequence number for an aggregate.
     */
    private Long getNextSequenceNumber(String aggregateId) {
        Long latestSequence = eventStoreRepository.findLatestSequenceNumberByAggregateId(aggregateId);
        return latestSequence != null ? latestSequence + 1 : 1L;
    }
    
    /**
     * Serialize event payload to JSON.
     */
    private String serializeEventData(DomainEvent event) throws JsonProcessingException {
        return objectMapper.writeValueAsString(event.getPayload());
    }
    
    /**
     * Serialize event metadata to JSON.
     */
    private String serializeMetadata(DomainEvent event) throws JsonProcessingException {
        return objectMapper.writeValueAsString(event.getMetadata());
    }
    
    /**
     * Retrieve all events for a specific aggregate.
     */
    @Transactional(readOnly = true)
    public EventStream getEventStream(String aggregateId) {
        var events = eventStoreRepository.findByAggregateIdOrderBySequenceNumberAsc(aggregateId);
        return new EventStream(aggregateId, events);
    }
    
    /**
     * Retrieve events for a specific aggregate after a given sequence number.
     */
    @Transactional(readOnly = true)
    public EventStream getEventStreamAfter(String aggregateId, Long sequenceNumber) {
        var events = eventStoreRepository.findByAggregateIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
                aggregateId, sequenceNumber);
        return new EventStream(aggregateId, events);
    }
    
    /**
     * Get events by correlation ID for tracing.
     */
    @Transactional(readOnly = true)
    public EventTrace getEventTrace(String correlationId) {
        var events = eventStoreRepository.findByCorrelationIdOrderByOccurredAtAsc(correlationId);
        return new EventTrace(correlationId, events);
    }
    
    /**
     * Asynchronously store event without blocking.
     */
    public CompletableFuture<Void> publishAsync(DomainEvent event) {
        return CompletableFuture.runAsync(() -> publish(event));
    }
    
    /**
     * Get recent events for monitoring.
     */
    @Transactional(readOnly = true)
    public RecentEvents getRecentEvents(LocalDateTime since, int limit) {
        var events = eventStoreRepository.findRecentEvents(since, 
                org.springframework.data.domain.PageRequest.of(0, limit));
        return new RecentEvents(since, events);
    }
    
    /**
     * Exception thrown when event store operations fail.
     */
    public static class EventStoreException extends RuntimeException {
        public EventStoreException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

package com.speechtotext.api.events.store;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for accessing and querying the event store.
 * 
 * Provides methods for storing and retrieving domain events
 * with various filtering and ordering capabilities for audit trails.
 */
@Repository
public interface EventStoreRepository extends JpaRepository<EventStoreEntry, UUID> {
    
    /**
     * Find all events for a specific aggregate, ordered by sequence number.
     */
    List<EventStoreEntry> findByAggregateIdOrderBySequenceNumberAsc(String aggregateId);
    
    /**
     * Find all events for a specific aggregate after a given sequence number.
     */
    List<EventStoreEntry> findByAggregateIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
        String aggregateId, Long sequenceNumber);
    
    /**
     * Find all events of a specific type, ordered by occurrence time.
     */
    List<EventStoreEntry> findByEventTypeOrderByOccurredAtDesc(String eventType);
    
    /**
     * Find all events for a specific aggregate type, ordered by occurrence time.
     */
    Page<EventStoreEntry> findByAggregateTypeOrderByOccurredAtDesc(String aggregateType, Pageable pageable);
    
    /**
     * Find all events with a specific correlation ID for tracing.
     */
    List<EventStoreEntry> findByCorrelationIdOrderByOccurredAtAsc(String correlationId);
    
    /**
     * Find all events initiated by a specific user/system.
     */
    Page<EventStoreEntry> findByInitiatedByOrderByOccurredAtDesc(String initiatedBy, Pageable pageable);
    
    /**
     * Find all events that occurred within a time range.
     */
    @Query("SELECT e FROM EventStoreEntry e WHERE e.occurredAt BETWEEN :startTime AND :endTime ORDER BY e.occurredAt DESC")
    Page<EventStoreEntry> findEventsBetweenTimestamps(
        @Param("startTime") LocalDateTime startTime, 
        @Param("endTime") LocalDateTime endTime, 
        Pageable pageable);
    
    /**
     * Find events by aggregate type and event type for specific queries.
     */
    List<EventStoreEntry> findByAggregateTypeAndEventTypeOrderByOccurredAtDesc(
        String aggregateType, String eventType);
    
    /**
     * Find the latest sequence number for a specific aggregate.
     */
    @Query("SELECT MAX(e.sequenceNumber) FROM EventStoreEntry e WHERE e.aggregateId = :aggregateId")
    Long findLatestSequenceNumberByAggregateId(@Param("aggregateId") String aggregateId);
    
    /**
     * Count events by event type for metrics.
     */
    long countByEventType(String eventType);
    
    /**
     * Count events by aggregate type for metrics.
     */
    long countByAggregateType(String aggregateType);
    
    /**
     * Find recent events for activity monitoring.
     */
    @Query("SELECT e FROM EventStoreEntry e WHERE e.occurredAt >= :since ORDER BY e.occurredAt DESC")
    List<EventStoreEntry> findRecentEvents(@Param("since") LocalDateTime since, Pageable pageable);
    
    /**
     * Find events for audit report generation.
     */
    @Query("SELECT e FROM EventStoreEntry e WHERE " +
           "(:aggregateType IS NULL OR e.aggregateType = :aggregateType) AND " +
           "(:eventType IS NULL OR e.eventType = :eventType) AND " +
           "(:correlationId IS NULL OR e.correlationId = :correlationId) AND " +
           "(:initiatedBy IS NULL OR e.initiatedBy = :initiatedBy) AND " +
           "e.occurredAt BETWEEN :startTime AND :endTime " +
           "ORDER BY e.occurredAt DESC")
    Page<EventStoreEntry> findEventsForAuditReport(
        @Param("aggregateType") String aggregateType,
        @Param("eventType") String eventType,
        @Param("correlationId") String correlationId,
        @Param("initiatedBy") String initiatedBy,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        Pageable pageable);
    
    /**
     * Check if events exist for a specific aggregate.
     */
    boolean existsByAggregateId(String aggregateId);
}

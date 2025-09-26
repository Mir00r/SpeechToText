package com.speechtotext.api.events.store;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a stream of events for a specific aggregate.
 */
public class EventStream {
    private final String aggregateId;
    private final List<EventStoreEntry> events;
    
    public EventStream(String aggregateId, List<EventStoreEntry> events) {
        this.aggregateId = aggregateId;
        this.events = events;
    }
    
    public String getAggregateId() {
        return aggregateId;
    }
    
    public List<EventStoreEntry> getEvents() {
        return events;
    }
    
    public int getEventCount() {
        return events.size();
    }
    
    public boolean isEmpty() {
        return events.isEmpty();
    }
    
    public LocalDateTime getFirstEventTime() {
        return events.isEmpty() ? null : events.get(0).getOccurredAt();
    }
    
    public LocalDateTime getLastEventTime() {
        return events.isEmpty() ? null : events.get(events.size() - 1).getOccurredAt();
    }
}

/**
 * Represents a trace of events with the same correlation ID.
 */
public class EventTrace {
    private final String correlationId;
    private final List<EventStoreEntry> events;
    
    public EventTrace(String correlationId, List<EventStoreEntry> events) {
        this.correlationId = correlationId;
        this.events = events;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public List<EventStoreEntry> getEvents() {
        return events;
    }
    
    public int getEventCount() {
        return events.size();
    }
    
    public boolean isEmpty() {
        return events.isEmpty();
    }
}

/**
 * Represents recent events for monitoring purposes.
 */
class RecentEvents {
    private final LocalDateTime since;
    private final List<EventStoreEntry> events;
    
    public RecentEvents(LocalDateTime since, List<EventStoreEntry> events) {
        this.since = since;
        this.events = events;
    }
    
    public LocalDateTime getSince() {
        return since;
    }
    
    public List<EventStoreEntry> getEvents() {
        return events;
    }
    
    public int getEventCount() {
        return events.size();
    }
    
    public boolean isEmpty() {
        return events.isEmpty();
    }
}

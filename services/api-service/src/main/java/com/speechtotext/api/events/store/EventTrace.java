package com.speechtotext.api.events.store;

import java.util.List;

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

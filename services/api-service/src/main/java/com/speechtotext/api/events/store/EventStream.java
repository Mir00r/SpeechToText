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

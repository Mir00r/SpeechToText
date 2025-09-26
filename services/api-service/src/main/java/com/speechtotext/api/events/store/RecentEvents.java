package com.speechtotext.api.events.store;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents recent events for monitoring purposes.
 */
public class RecentEvents {
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

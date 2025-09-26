package com.speechtotext.api.events.publisher;

import com.speechtotext.api.events.DomainEvent;

/**
 * Interface for publishing domain events.
 * 
 * This abstraction allows for different event publishing strategies
 * such as synchronous processing, asynchronous messaging, or event streaming.
 */
public interface EventPublisher {
    
    /**
     * Publish a single domain event.
     * 
     * @param event The domain event to publish
     */
    void publish(DomainEvent event);
    
    /**
     * Publish multiple domain events as a batch.
     * 
     * @param events The domain events to publish
     */
    void publishAll(DomainEvent... events);
    
    /**
     * Publish multiple domain events as a batch.
     * 
     * @param events The domain events to publish
     */
    void publishAll(Iterable<DomainEvent> events);
}

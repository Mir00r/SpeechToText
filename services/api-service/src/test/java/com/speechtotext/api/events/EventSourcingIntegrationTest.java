package com.speechtotext.api.events;

import com.speechtotext.api.events.factory.EventFactory;
import com.speechtotext.api.events.job.JobCreatedEvent;
import com.speechtotext.api.events.publisher.EventPublisher;
import com.speechtotext.api.events.store.EventStoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the event sourcing system
 */
@SpringBootTest
@ActiveProfiles("test")
public class EventSourcingIntegrationTest {

    @Autowired
    private EventFactory eventFactory;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private EventStoreService eventStoreService;

    @Test
    public void testJobCreatedEventCreation() {
        // Test creating a job created event
        JobCreatedEvent event = eventFactory.createJobCreatedEvent(
            "test-job-123",
            "test-file.wav",
            "test-file.wav",
            "s3://bucket/test-file.wav",
            "whisper",
            "en",
            "high",
            1024L,
            BigDecimal.valueOf(30.0),
            false,
            false,
            "127.0.0.1",
            "test-user-agent"
        );

        assertNotNull(event);
        assertEquals("test-job-123", event.getAggregateId());
        assertEquals("JobCreatedEvent", event.getEventType());
        assertNotNull(event.getEventId());
        assertNotNull(event.getOccurredAt());
    }

    @Test
    public void testEventPublishing() {
        // Test publishing an event
        DomainEvent event = eventFactory.createJobCreatedEvent(
            "test-job-456",
            "another-file.wav",
            "another-file.wav", 
            "s3://bucket/another-file.wav",
            "whisper",
            "es", 
            "medium",
            2048L,
            BigDecimal.valueOf(60.0),
            true,
            false,
            "192.168.1.1",
            "test-agent-2"
        );

        assertDoesNotThrow(() -> {
            eventPublisher.publish(event);
        });
    }

    @Test
    public void testEventStoreService() {
        // Test event store functionality
        assertNotNull(eventStoreService);
        // Additional tests could be added here to verify persistence
    }
}

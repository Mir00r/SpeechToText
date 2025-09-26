package com.speechtotext.api.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for circuit breaker event listeners.
 * Logs important circuit breaker state changes for monitoring.
 */
@Configuration
public class CircuitBreakerEventConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerEventConfig.class);
    
    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerEventConsumer() {
        return new RegistryEventConsumer<CircuitBreaker>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
                CircuitBreaker circuitBreaker = entryAddedEvent.getAddedEntry();
                logger.info("Circuit breaker '{}' added to registry", circuitBreaker.getName());
                
                // Add event listeners to the circuit breaker
                circuitBreaker.getEventPublisher()
                    .onStateTransition(event -> 
                        logger.warn("Circuit breaker '{}' transitioned from {} to {}", 
                            circuitBreaker.getName(), event.getStateTransition().getFromState(), 
                            event.getStateTransition().getToState()))
                    .onFailureRateExceeded(event -> 
                        logger.error("Circuit breaker '{}' failure rate exceeded: {}%", 
                            circuitBreaker.getName(), event.getFailureRate()))
                    .onSlowCallRateExceeded(event -> 
                        logger.warn("Circuit breaker '{}' slow call rate exceeded: {}%", 
                            circuitBreaker.getName(), event.getSlowCallRate()))
                    .onCallNotPermitted(event -> 
                        logger.warn("Circuit breaker '{}' rejected call", circuitBreaker.getName()))
                    .onError(event -> 
                        logger.debug("Circuit breaker '{}' recorded error: {}", 
                            circuitBreaker.getName(), event.getThrowable().getClass().getSimpleName()))
                    .onSuccess(event -> 
                        logger.debug("Circuit breaker '{}' recorded success", circuitBreaker.getName()));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemoveEvent) {
                logger.info("Circuit breaker '{}' removed from registry", entryRemoveEvent.getRemovedEntry().getName());
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
                logger.info("Circuit breaker '{}' replaced in registry", entryReplacedEvent.getNewEntry().getName());
            }
        };
    }
}

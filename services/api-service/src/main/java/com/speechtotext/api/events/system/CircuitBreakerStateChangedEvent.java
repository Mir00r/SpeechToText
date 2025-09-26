package com.speechtotext.api.events.system;

import com.speechtotext.api.events.BaseDomainEvent;
import com.speechtotext.api.events.DomainEvent;

import java.util.Map;

/**
 * Event fired when a circuit breaker state changes.
 * 
 * This event captures circuit breaker state transitions which are
 * critical for understanding system health and service availability.
 */
public class CircuitBreakerStateChangedEvent extends BaseDomainEvent {
    
    private final CircuitBreakerStateChangedPayload payload;
    
    private CircuitBreakerStateChangedEvent(Builder builder) {
        super(builder);
        this.payload = new CircuitBreakerStateChangedPayload(
                builder.circuitBreakerName,
                builder.previousState,
                builder.newState,
                builder.reason,
                builder.failureRate,
                builder.slowCallRate,
                builder.numberOfCalls,
                builder.numberOfFailedCalls,
                builder.serviceName
        );
    }
    
    @Override
    public Object getPayload() {
        return payload;
    }
    
    /**
     * Payload containing circuit breaker state change details.
     */
    public static class CircuitBreakerStateChangedPayload {
        private final String circuitBreakerName;
        private final String previousState;
        private final String newState;
        private final String reason;
        private final Float failureRate;
        private final Float slowCallRate;
        private final Integer numberOfCalls;
        private final Integer numberOfFailedCalls;
        private final String serviceName;
        
        public CircuitBreakerStateChangedPayload(String circuitBreakerName, String previousState,
                                                 String newState, String reason, Float failureRate,
                                                 Float slowCallRate, Integer numberOfCalls,
                                                 Integer numberOfFailedCalls, String serviceName) {
            this.circuitBreakerName = circuitBreakerName;
            this.previousState = previousState;
            this.newState = newState;
            this.reason = reason;
            this.failureRate = failureRate;
            this.slowCallRate = slowCallRate;
            this.numberOfCalls = numberOfCalls;
            this.numberOfFailedCalls = numberOfFailedCalls;
            this.serviceName = serviceName;
        }
        
        // Getters
        public String getCircuitBreakerName() { return circuitBreakerName; }
        public String getPreviousState() { return previousState; }
        public String getNewState() { return newState; }
        public String getReason() { return reason; }
        public Float getFailureRate() { return failureRate; }
        public Float getSlowCallRate() { return slowCallRate; }
        public Integer getNumberOfCalls() { return numberOfCalls; }
        public Integer getNumberOfFailedCalls() { return numberOfFailedCalls; }
        public String getServiceName() { return serviceName; }
    }
    
    /**
     * Builder for creating CircuitBreakerStateChangedEvent instances.
     */
    public static class Builder extends BaseDomainEvent.Builder {
        private String circuitBreakerName;
        private String previousState;
        private String newState;
        private String reason;
        private Float failureRate;
        private Float slowCallRate;
        private Integer numberOfCalls;
        private Integer numberOfFailedCalls;
        private String serviceName;
        
        public Builder() {
            eventType("CircuitBreakerStateChanged");
            aggregateType("SystemHealth");
        }
        
        public Builder circuitBreakerName(String circuitBreakerName) {
            this.circuitBreakerName = circuitBreakerName;
            return this;
        }
        
        public Builder previousState(String previousState) {
            this.previousState = previousState;
            return this;
        }
        
        public Builder newState(String newState) {
            this.newState = newState;
            return this;
        }
        
        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }
        
        public Builder failureRate(Float failureRate) {
            this.failureRate = failureRate;
            return this;
        }
        
        public Builder slowCallRate(Float slowCallRate) {
            this.slowCallRate = slowCallRate;
            return this;
        }
        
        public Builder numberOfCalls(Integer numberOfCalls) {
            this.numberOfCalls = numberOfCalls;
            return this;
        }
        
        public Builder numberOfFailedCalls(Integer numberOfFailedCalls) {
            this.numberOfFailedCalls = numberOfFailedCalls;
            return this;
        }
        
        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }
        
        @Override
        public Builder aggregateId(String aggregateId) {
            super.aggregateId(aggregateId);
            return this;
        }
        
        @Override
        public Builder correlationId(String correlationId) {
            super.correlationId(correlationId);
            return this;
        }
        
        @Override
        public Builder initiatedBy(String initiatedBy) {
            super.initiatedBy(initiatedBy);
            return this;
        }
        
        @Override
        public Builder metadata(String key, Object value) {
            super.metadata(key, value);
            return this;
        }
        
        @Override
        public Builder metadata(Map<String, Object> metadata) {
            super.metadata(metadata);
            return this;
        }
        
        @Override
        public Builder sequenceNumber(Long sequenceNumber) {
            super.sequenceNumber(sequenceNumber);
            return this;
        }
        
        @Override
        public DomainEvent build() {
            if (circuitBreakerName == null || newState == null) {
                throw new IllegalArgumentException("circuitBreakerName and newState are required");
            }
            return new CircuitBreakerStateChangedEvent(this);
        }
    }
}

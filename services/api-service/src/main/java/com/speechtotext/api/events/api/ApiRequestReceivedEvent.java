package com.speechtotext.api.events.api;

import com.speechtotext.api.events.BaseDomainEvent;
import com.speechtotext.api.events.DomainEvent;

import java.util.Map;

/**
 * Event fired when an API request is received.
 * 
 * This event captures incoming API requests for audit trail,
 * monitoring, and analytics purposes.
 */
public class ApiRequestReceivedEvent extends BaseDomainEvent {
    
    private final ApiRequestReceivedPayload payload;
    
    private ApiRequestReceivedEvent(Builder builder) {
        super(builder);
        this.payload = new ApiRequestReceivedPayload(
                builder.requestId,
                builder.method,
                builder.path,
                builder.clientIp,
                builder.userAgent,
                builder.contentType,
                builder.contentLength,
                builder.authenticationInfo,
                builder.queryParams
        );
    }
    
    @Override
    public Object getPayload() {
        return payload;
    }
    
    /**
     * Payload containing API request details.
     */
    public static class ApiRequestReceivedPayload {
        private final String requestId;
        private final String method;
        private final String path;
        private final String clientIp;
        private final String userAgent;
        private final String contentType;
        private final Long contentLength;
        private final String authenticationInfo;
        private final Map<String, String> queryParams;
        
        public ApiRequestReceivedPayload(String requestId, String method, String path,
                                        String clientIp, String userAgent, String contentType,
                                        Long contentLength, String authenticationInfo,
                                        Map<String, String> queryParams) {
            this.requestId = requestId;
            this.method = method;
            this.path = path;
            this.clientIp = clientIp;
            this.userAgent = userAgent;
            this.contentType = contentType;
            this.contentLength = contentLength;
            this.authenticationInfo = authenticationInfo;
            this.queryParams = queryParams;
        }
        
        // Getters
        public String getRequestId() { return requestId; }
        public String getMethod() { return method; }
        public String getPath() { return path; }
        public String getClientIp() { return clientIp; }
        public String getUserAgent() { return userAgent; }
        public String getContentType() { return contentType; }
        public Long getContentLength() { return contentLength; }
        public String getAuthenticationInfo() { return authenticationInfo; }
        public Map<String, String> getQueryParams() { return queryParams; }
    }
    
    /**
     * Builder for creating ApiRequestReceivedEvent instances.
     */
    public static class Builder extends BaseDomainEvent.Builder {
        private String requestId;
        private String method;
        private String path;
        private String clientIp;
        private String userAgent;
        private String contentType;
        private Long contentLength;
        private String authenticationInfo;
        private Map<String, String> queryParams;
        
        public Builder() {
            eventType("ApiRequestReceived");
            aggregateType("ApiRequest");
        }
        
        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }
        
        public Builder method(String method) {
            this.method = method;
            return this;
        }
        
        public Builder path(String path) {
            this.path = path;
            return this;
        }
        
        public Builder clientIp(String clientIp) {
            this.clientIp = clientIp;
            return this;
        }
        
        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }
        
        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }
        
        public Builder contentLength(Long contentLength) {
            this.contentLength = contentLength;
            return this;
        }
        
        public Builder authenticationInfo(String authenticationInfo) {
            this.authenticationInfo = authenticationInfo;
            return this;
        }
        
        public Builder queryParams(Map<String, String> queryParams) {
            this.queryParams = queryParams;
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
            if (method == null || path == null) {
                throw new IllegalArgumentException("method and path are required");
            }
            return new ApiRequestReceivedEvent(this);
        }
    }
}

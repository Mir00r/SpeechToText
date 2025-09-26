package com.speechtotext.api.trace;

import org.slf4j.MDC;

import java.util.Map;
import java.util.UUID;

/**
 * Utility class for managing request tracing context throughout the application lifecycle.
 * Provides methods for generating trace IDs, managing MDC context, and correlating requests.
 */
public final class TraceContext {
    
    /**
     * Generate a new correlation ID for request tracking.
     * 
     * @return A unique correlation ID
     */
    public static String generateCorrelationId() {
        return "corr-" + UUID.randomUUID().toString();
    }
    
    /**
     * Generate a new request ID for internal request tracking.
     * 
     * @return A unique request ID
     */
    public static String generateRequestId() {
        return "req-" + UUID.randomUUID().toString();
    }
    
    /**
     * Generate a new trace ID for distributed tracing.
     * 
     * @return A unique trace ID
     */
    public static String generateTraceId() {
        return "trace-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Initialize trace context with correlation and request IDs.
     * 
     * @param correlationId Existing correlation ID or null to generate new one
     * @param requestId Existing request ID or null to generate new one
     * @return The correlation ID being used
     */
    public static String initializeTraceContext(String correlationId, String requestId) {
        String corrId = correlationId != null && !correlationId.trim().isEmpty() 
                       ? correlationId 
                       : generateCorrelationId();
        
        String reqId = requestId != null && !requestId.trim().isEmpty() 
                      ? requestId 
                      : generateRequestId();
        
        String traceId = generateTraceId();
        
        MDC.put(TraceConstants.CORRELATION_ID, corrId);
        MDC.put(TraceConstants.REQUEST_ID, reqId);
        MDC.put(TraceConstants.TRACE_ID, traceId);
        
        return corrId;
    }
    
    /**
     * Set operation context for the current request.
     * 
     * @param operation The operation being performed
     */
    public static void setOperation(String operation) {
        MDC.put(TraceConstants.OPERATION, operation);
    }
    
    /**
     * Set job context for transcription operations.
     * 
     * @param jobId The job ID
     */
    public static void setJobContext(String jobId) {
        MDC.put(TraceConstants.JOB_ID, jobId);
    }
    
    /**
     * Set file context for upload operations.
     * 
     * @param fileName The file name
     * @param fileSize The file size in bytes
     */
    public static void setFileContext(String fileName, long fileSize) {
        MDC.put(TraceConstants.FILE_NAME, fileName);
        MDC.put(TraceConstants.FILE_SIZE, String.valueOf(fileSize));
    }
    
    /**
     * Set transcription parameters context.
     * 
     * @param language The language code
     * @param model The model name
     */
    public static void setTranscriptionContext(String language, String model) {
        if (language != null) {
            MDC.put(TraceConstants.LANGUAGE, language);
        }
        if (model != null) {
            MDC.put(TraceConstants.MODEL, model);
        }
    }
    
    /**
     * Set user context information.
     * 
     * @param userId The user ID
     * @param clientIp The client IP address
     */
    public static void setUserContext(String userId, String clientIp) {
        if (userId != null) {
            MDC.put(TraceConstants.USER_ID, userId);
        }
        if (clientIp != null) {
            MDC.put(TraceConstants.CLIENT_IP, clientIp);
        }
    }
    
    /**
     * Set request context information.
     * 
     * @param method The HTTP method
     * @param uri The request URI
     * @param userAgent The user agent
     */
    public static void setRequestContext(String method, String uri, String userAgent) {
        if (method != null) {
            MDC.put(TraceConstants.REQUEST_METHOD, method);
        }
        if (uri != null) {
            MDC.put(TraceConstants.REQUEST_URI, uri);
        }
        if (userAgent != null) {
            MDC.put(TraceConstants.USER_AGENT, userAgent);
        }
    }
    
    /**
     * Set response context information.
     * 
     * @param status The HTTP response status
     * @param responseSize The response size in bytes
     * @param durationMs The request duration in milliseconds
     */
    public static void setResponseContext(int status, long responseSize, long durationMs) {
        MDC.put(TraceConstants.RESPONSE_STATUS, String.valueOf(status));
        MDC.put(TraceConstants.RESPONSE_SIZE, String.valueOf(responseSize));
        MDC.put(TraceConstants.DURATION_MS, String.valueOf(durationMs));
    }
    
    /**
     * Get the current correlation ID from MDC.
     * 
     * @return The correlation ID or null if not set
     */
    public static String getCorrelationId() {
        return MDC.get(TraceConstants.CORRELATION_ID);
    }
    
    /**
     * Get the current request ID from MDC.
     * 
     * @return The request ID or null if not set
     */
    public static String getRequestId() {
        return MDC.get(TraceConstants.REQUEST_ID);
    }
    
    /**
     * Get the current trace ID from MDC.
     * 
     * @return The trace ID or null if not set
     */
    public static String getTraceId() {
        return MDC.get(TraceConstants.TRACE_ID);
    }
    
    /**
     * Get all current MDC context as a map.
     * 
     * @return Map of MDC context or null if empty
     */
    public static Map<String, String> getCurrentContext() {
        return MDC.getCopyOfContextMap();
    }
    
    /**
     * Set MDC context from a map (useful for thread propagation).
     * 
     * @param context The context map to set
     */
    public static void setContext(Map<String, String> context) {
        if (context != null && !context.isEmpty()) {
            MDC.setContextMap(context);
        }
    }
    
    /**
     * Clear all MDC context.
     */
    public static void clearContext() {
        MDC.clear();
    }
    
    /**
     * Clear specific MDC keys related to operation-specific context
     * (but keep request-level context like correlation ID).
     */
    public static void clearOperationContext() {
        MDC.remove(TraceConstants.OPERATION);
        MDC.remove(TraceConstants.JOB_ID);
        MDC.remove(TraceConstants.FILE_NAME);
        MDC.remove(TraceConstants.FILE_SIZE);
        MDC.remove(TraceConstants.LANGUAGE);
        MDC.remove(TraceConstants.MODEL);
        MDC.remove(TraceConstants.DURATION_MS);
        MDC.remove(TraceConstants.RESPONSE_STATUS);
        MDC.remove(TraceConstants.RESPONSE_SIZE);
    }
    
    private TraceContext() {
        throw new IllegalStateException("Utility class");
    }
}

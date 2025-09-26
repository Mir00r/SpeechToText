package com.speechtotext.api.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for request tracing functionality.
 */
class TraceContextTest {

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void testGenerateCorrelationId() {
        String correlationId = TraceContext.generateCorrelationId();
        assertNotNull(correlationId);
        assertTrue(correlationId.startsWith("corr-"));
    }

    @Test
    void testGenerateRequestId() {
        String requestId = TraceContext.generateRequestId();
        assertNotNull(requestId);
        assertTrue(requestId.startsWith("req-"));
    }

    @Test
    void testGenerateTraceId() {
        String traceId = TraceContext.generateTraceId();
        assertNotNull(traceId);
        assertTrue(traceId.startsWith("trace-"));
    }

    @Test
    void testInitializeTraceContext() {
        String correlationId = TraceContext.initializeTraceContext(null, null);
        
        assertNotNull(correlationId);
        assertTrue(correlationId.startsWith("corr-"));
        assertEquals(correlationId, TraceContext.getCorrelationId());
        assertNotNull(TraceContext.getRequestId());
        assertNotNull(TraceContext.getTraceId());
    }

    @Test
    void testInitializeTraceContextWithExistingIds() {
        String existingCorrelationId = "existing-corr-123";
        String existingRequestId = "existing-req-456";
        
        String returnedCorrelationId = TraceContext.initializeTraceContext(existingCorrelationId, existingRequestId);
        
        assertEquals(existingCorrelationId, returnedCorrelationId);
        assertEquals(existingCorrelationId, TraceContext.getCorrelationId());
        assertEquals(existingRequestId, TraceContext.getRequestId());
        assertNotNull(TraceContext.getTraceId());
    }

    @Test
    void testSetOperationContext() {
        TraceContext.setOperation("test-operation");
        
        assertEquals("test-operation", MDC.get(TraceConstants.OPERATION));
    }

    @Test
    void testSetJobContext() {
        String jobId = "job-123";
        TraceContext.setJobContext(jobId);
        
        assertEquals(jobId, MDC.get(TraceConstants.JOB_ID));
    }

    @Test
    void testSetFileContext() {
        String fileName = "test.wav";
        long fileSize = 1024;
        
        TraceContext.setFileContext(fileName, fileSize);
        
        assertEquals(fileName, MDC.get(TraceConstants.FILE_NAME));
        assertEquals(String.valueOf(fileSize), MDC.get(TraceConstants.FILE_SIZE));
    }

    @Test
    void testSetTranscriptionContext() {
        String language = "en";
        String model = "base";
        
        TraceContext.setTranscriptionContext(language, model);
        
        assertEquals(language, MDC.get(TraceConstants.LANGUAGE));
        assertEquals(model, MDC.get(TraceConstants.MODEL));
    }

    @Test
    void testSetUserContext() {
        String userId = "user-123";
        String clientIp = "192.168.1.100";
        
        TraceContext.setUserContext(userId, clientIp);
        
        assertEquals(userId, MDC.get(TraceConstants.USER_ID));
        assertEquals(clientIp, MDC.get(TraceConstants.CLIENT_IP));
    }

    @Test
    void testSetRequestContext() {
        String method = "POST";
        String uri = "/api/v1/transcriptions";
        String userAgent = "Test-Client/1.0";
        
        TraceContext.setRequestContext(method, uri, userAgent);
        
        assertEquals(method, MDC.get(TraceConstants.REQUEST_METHOD));
        assertEquals(uri, MDC.get(TraceConstants.REQUEST_URI));
        assertEquals(userAgent, MDC.get(TraceConstants.USER_AGENT));
    }

    @Test
    void testSetResponseContext() {
        int status = 200;
        long responseSize = 512;
        long durationMs = 150;
        
        TraceContext.setResponseContext(status, responseSize, durationMs);
        
        assertEquals(String.valueOf(status), MDC.get(TraceConstants.RESPONSE_STATUS));
        assertEquals(String.valueOf(responseSize), MDC.get(TraceConstants.RESPONSE_SIZE));
        assertEquals(String.valueOf(durationMs), MDC.get(TraceConstants.DURATION_MS));
    }

    @Test
    void testClearContext() {
        TraceContext.initializeTraceContext(null, null);
        TraceContext.setOperation("test");
        TraceContext.setJobContext("job-123");
        
        assertFalse(MDC.getCopyOfContextMap().isEmpty());
        
        TraceContext.clearContext();
        
        assertNull(MDC.getCopyOfContextMap());
    }

    @Test
    void testClearOperationContext() {
        TraceContext.initializeTraceContext(null, null);
        TraceContext.setOperation("test");
        TraceContext.setJobContext("job-123");
        TraceContext.setFileContext("test.wav", 1024);
        
        String correlationId = TraceContext.getCorrelationId();
        
        TraceContext.clearOperationContext();
        
        // Request-level context should remain
        assertEquals(correlationId, TraceContext.getCorrelationId());
        assertNotNull(TraceContext.getRequestId());
        assertNotNull(TraceContext.getTraceId());
        
        // Operation-level context should be cleared
        assertNull(MDC.get(TraceConstants.OPERATION));
        assertNull(MDC.get(TraceConstants.JOB_ID));
        assertNull(MDC.get(TraceConstants.FILE_NAME));
        assertNull(MDC.get(TraceConstants.FILE_SIZE));
    }

    @Test
    void testContextPreservation() {
        TraceContext.initializeTraceContext(null, null);
        var originalContext = TraceContext.getCurrentContext();
        
        TraceContext.clearContext();
        assertNull(TraceContext.getCurrentContext());
        
        TraceContext.setContext(originalContext);
        assertEquals(originalContext, TraceContext.getCurrentContext());
    }
}

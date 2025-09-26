package com.speechtotext.api.trace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Utility class for adding tracing to external service calls and operations.
 * Provides methods to wrap operations with trace context and propagate correlation IDs.
 */
@Component
public class TracingHelper {
    
    private static final Logger logger = LoggerFactory.getLogger(TracingHelper.class);
    
    /**
     * Execute an operation with trace context and automatic performance measurement.
     * 
     * @param operation The operation name for logging
     * @param callable The operation to execute
     * @param <T> The return type of the operation
     * @return The result of the operation
     * @throws Exception Any exception thrown by the operation
     */
    public <T> T executeWithTrace(String operation, Callable<T> callable) throws Exception {
        long startTime = System.currentTimeMillis();
        String originalOperation = TraceContext.getCurrentContext() != null 
            ? TraceContext.getCurrentContext().get(TraceConstants.OPERATION) 
            : null;
        
        try {
            TraceContext.setOperation(operation);
            logger.debug("Starting traced operation: {} [correlationId={}]", 
                        operation, TraceContext.getCorrelationId());
            
            T result = callable.call();
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Traced operation completed successfully: {} - Duration: {}ms [correlationId={}]",
                       operation, duration, TraceContext.getCorrelationId());
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Traced operation failed: {} - Duration: {}ms - Error: {} [correlationId={}]",
                        operation, duration, e.getMessage(), TraceContext.getCorrelationId(), e);
            throw e;
            
        } finally {
            // Restore original operation context
            if (originalOperation != null) {
                TraceContext.setOperation(originalOperation);
            } else {
                TraceContext.clearOperationContext();
            }
        }
    }
    
    /**
     * Execute a void operation with trace context and automatic performance measurement.
     * 
     * @param operation The operation name for logging
     * @param runnable The operation to execute
     */
    public void executeWithTrace(String operation, Runnable runnable) {
        long startTime = System.currentTimeMillis();
        String originalOperation = TraceContext.getCurrentContext() != null 
            ? TraceContext.getCurrentContext().get(TraceConstants.OPERATION) 
            : null;
        
        try {
            TraceContext.setOperation(operation);
            logger.debug("Starting traced operation: {} [correlationId={}]", 
                        operation, TraceContext.getCorrelationId());
            
            runnable.run();
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Traced operation completed successfully: {} - Duration: {}ms [correlationId={}]",
                       operation, duration, TraceContext.getCorrelationId());
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Traced operation failed: {} - Duration: {}ms - Error: {} [correlationId={}]",
                        operation, duration, e.getMessage(), TraceContext.getCorrelationId(), e);
            throw e;
            
        } finally {
            // Restore original operation context
            if (originalOperation != null) {
                TraceContext.setOperation(originalOperation);
            } else {
                TraceContext.clearOperationContext();
            }
        }
    }
    
    /**
     * Create HTTP headers with correlation ID for external service calls.
     * 
     * @return HttpHeaders with tracing headers populated
     */
    public HttpHeaders createTracingHeaders() {
        HttpHeaders headers = new HttpHeaders();
        
        String correlationId = TraceContext.getCorrelationId();
        String requestId = TraceContext.getRequestId();
        String traceId = TraceContext.getTraceId();
        
        if (correlationId != null) {
            headers.set(TraceConstants.CORRELATION_ID_HEADER, correlationId);
        }
        if (requestId != null) {
            headers.set(TraceConstants.REQUEST_ID_HEADER, requestId);
        }
        if (traceId != null) {
            headers.set(TraceConstants.TRACE_ID_HEADER, traceId);
        }
        
        return headers;
    }
    
    /**
     * Add tracing headers to existing HTTP headers.
     * 
     * @param headers The headers to add tracing headers to
     */
    public void addTracingHeaders(HttpHeaders headers) {
        String correlationId = TraceContext.getCorrelationId();
        String requestId = TraceContext.getRequestId();
        String traceId = TraceContext.getTraceId();
        
        if (correlationId != null) {
            headers.set(TraceConstants.CORRELATION_ID_HEADER, correlationId);
        }
        if (requestId != null) {
            headers.set(TraceConstants.REQUEST_ID_HEADER, requestId);
        }
        if (traceId != null) {
            headers.set(TraceConstants.TRACE_ID_HEADER, traceId);
        }
    }
    
    /**
     * Execute a supplier operation with S3 context.
     * 
     * @param fileName The file name being processed
     * @param operation The S3 operation (upload/download)
     * @param supplier The operation to execute
     * @param <T> The return type
     * @return The result of the operation
     */
    public <T> T executeS3Operation(String fileName, String operation, Supplier<T> supplier) {
        long startTime = System.currentTimeMillis();
        String fullOperation = TraceConstants.OP_S3_UPLOAD.equals(operation) ? 
                              TraceConstants.OP_S3_UPLOAD : TraceConstants.OP_S3_DOWNLOAD;
        
        String originalOperation = TraceContext.getCurrentContext() != null 
            ? TraceContext.getCurrentContext().get(TraceConstants.OPERATION) 
            : null;
        
        try {
            TraceContext.setOperation(fullOperation);
            TraceContext.setFileContext(fileName, 0); // Size will be set by caller if available
            
            logger.debug("Starting S3 operation: {} for file: {} [correlationId={}]", 
                        operation, fileName, TraceContext.getCorrelationId());
            
            T result = supplier.get();
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("S3 operation completed: {} for file: {} - Duration: {}ms [correlationId={}]",
                       operation, fileName, duration, TraceContext.getCorrelationId());
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("S3 operation failed: {} for file: {} - Duration: {}ms - Error: {} [correlationId={}]",
                        operation, fileName, duration, e.getMessage(), TraceContext.getCorrelationId(), e);
            throw e;
            
        } finally {
            // Restore original operation context
            if (originalOperation != null) {
                TraceContext.setOperation(originalOperation);
            } else {
                TraceContext.clearOperationContext();
            }
        }
    }
    
    /**
     * Execute a database operation with trace context.
     * 
     * @param operation The database operation description
     * @param supplier The operation to execute
     * @param <T> The return type
     * @return The result of the operation
     */
    public <T> T executeDatabaseOperation(String operation, Supplier<T> supplier) {
        return executeWithTraceSupplier(TraceConstants.OP_DATABASE_OPERATION + ":" + operation, supplier);
    }
    
    /**
     * Execute a supplier operation with trace context and automatic performance measurement.
     * 
     * @param operation The operation name for logging
     * @param supplier The operation to execute
     * @param <T> The return type
     * @return The result of the operation
     */
    public <T> T executeWithTraceSupplier(String operation, Supplier<T> supplier) {
        long startTime = System.currentTimeMillis();
        String originalOperation = TraceContext.getCurrentContext() != null 
            ? TraceContext.getCurrentContext().get(TraceConstants.OPERATION) 
            : null;
        
        try {
            TraceContext.setOperation(operation);
            logger.debug("Starting traced operation: {} [correlationId={}]", 
                        operation, TraceContext.getCorrelationId());
            
            T result = supplier.get();
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Traced operation completed successfully: {} - Duration: {}ms [correlationId={}]",
                       operation, duration, TraceContext.getCorrelationId());
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Traced operation failed: {} - Duration: {}ms - Error: {} [correlationId={}]",
                        operation, duration, e.getMessage(), TraceContext.getCorrelationId(), e);
            throw e;
            
        } finally {
            // Restore original operation context
            if (originalOperation != null) {
                TraceContext.setOperation(originalOperation);
            } else {
                TraceContext.clearOperationContext();
            }
        }
    }
}

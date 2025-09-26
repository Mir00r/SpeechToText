package com.speechtotext.api.trace;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Controller-level request interceptor that adds operation-specific tracing
 * and performance metrics for REST endpoints.
 */
@Component
public class RequestTracingInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestTracingInterceptor.class);
    
    private static final String START_TIME_ATTRIBUTE = "startTime";
    
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler)
            throws Exception {
        
        // Record start time for performance measurement
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        
        // Set operation context based on the handler
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            String operation = determineOperation(request, handlerMethod);
            TraceContext.setOperation(operation);
            
            logger.debug("Starting operation: {} [method={}, class={}]", 
                        operation, 
                        handlerMethod.getMethod().getName(),
                        handlerMethod.getBeanType().getSimpleName());
        }
        
        return true;
    }
    
    @Override
    public void postHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler,
                          @Nullable ModelAndView modelAndView) throws Exception {
        // This method is called after the controller method but before the view is rendered
        // Can be used for additional processing if needed
    }
    
    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler,
                               @Nullable Exception ex) throws Exception {
        
        Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            
            if (handler instanceof HandlerMethod) {
                String operation = TraceContext.getCurrentContext() != null 
                    ? TraceContext.getCurrentContext().get(TraceConstants.OPERATION) 
                    : "unknown";
                
                if (ex != null) {
                    logger.error("Operation failed: {} - Duration: {}ms - Error: {} [correlationId={}]",
                                operation, duration, ex.getMessage(), TraceContext.getCorrelationId(), ex);
                } else {
                    logger.info("Operation completed: {} - Duration: {}ms - Status: {} [correlationId={}]",
                               operation, duration, response.getStatus(), TraceContext.getCorrelationId());
                }
            }
        }
        
        // Clear operation-specific context but keep request-level context
        TraceContext.clearOperationContext();
    }
    
    /**
     * Determine the operation name based on the request and handler method.
     */
    private String determineOperation(HttpServletRequest request, HandlerMethod handlerMethod) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String methodName = handlerMethod.getMethod().getName();
        
        // Map common patterns to operation names
        if (uri.contains("/transcriptions")) {
            if (method.equals("POST")) {
                return TraceConstants.OP_TRANSCRIPTION_CREATE;
            } else if (method.equals("GET")) {
                if (uri.contains("/download")) {
                    return TraceConstants.OP_TRANSCRIPTION_DOWNLOAD;
                } else if (uri.matches(".*transcriptions/[^/]+$")) {
                    return TraceConstants.OP_TRANSCRIPTION_GET;
                } else {
                    return TraceConstants.OP_TRANSCRIPTION_LIST;
                }
            } else if (method.equals("DELETE")) {
                return TraceConstants.OP_TRANSCRIPTION_DELETE;
            }
        }
        
        if (uri.contains("/models")) {
            return TraceConstants.OP_MODEL_SELECTION;
        }
        
        if (uri.contains("/callback")) {
            return TraceConstants.OP_CALLBACK_PROCESSING;
        }
        
        // Fallback: use method name
        return methodName.toLowerCase().replace("_", "-");
    }
}

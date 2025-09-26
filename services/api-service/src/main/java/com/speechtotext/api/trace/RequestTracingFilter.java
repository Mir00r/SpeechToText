package com.speechtotext.api.trace;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Request tracing filter that automatically initializes and manages trace context
 * for all incoming HTTP requests. This filter runs early in the filter chain to
 * ensure trace context is available throughout the request lifecycle.
 */
@Component
@Order(1) // Execute early in the filter chain
public class RequestTracingFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestTracingFilter.class);
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("Initializing RequestTracingFilter");
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Initialize trace context
            String existingCorrelationId = httpRequest.getHeader(TraceConstants.CORRELATION_ID_HEADER);
            String existingRequestId = httpRequest.getHeader(TraceConstants.REQUEST_ID_HEADER);
            
            String correlationId = TraceContext.initializeTraceContext(existingCorrelationId, existingRequestId);
            
            // Set request context
            TraceContext.setRequestContext(
                httpRequest.getMethod(),
                httpRequest.getRequestURI(),
                httpRequest.getHeader("User-Agent")
            );
            
            // Set user context
            String clientIp = extractClientIp(httpRequest);
            String userId = httpRequest.getHeader(TraceConstants.USER_ID_HEADER);
            TraceContext.setUserContext(userId, clientIp);
            
            // Add trace headers to response
            httpResponse.setHeader(TraceConstants.CORRELATION_ID_HEADER, correlationId);
            httpResponse.setHeader(TraceConstants.REQUEST_ID_HEADER, TraceContext.getRequestId());
            httpResponse.setHeader(TraceConstants.TRACE_ID_HEADER, TraceContext.getTraceId());
            
            logger.debug("Request started: {} {} [correlationId={}]", 
                        httpRequest.getMethod(), httpRequest.getRequestURI(), correlationId);
            
            // Process request
            chain.doFilter(request, response);
            
        } finally {
            // Calculate duration and set response context
            long duration = System.currentTimeMillis() - startTime;
            
            TraceContext.setResponseContext(
                httpResponse.getStatus(),
                0, // Response size - would need response wrapper to capture actual size
                duration
            );
            
            logger.info("Request completed: {} {} - Status: {} - Duration: {}ms [correlationId={}]",
                       httpRequest.getMethod(), 
                       httpRequest.getRequestURI(),
                       httpResponse.getStatus(),
                       duration,
                       TraceContext.getCorrelationId());
            
            // Clear trace context
            TraceContext.clearContext();
        }
    }
    
    @Override
    public void destroy() {
        logger.info("Destroying RequestTracingFilter");
    }
    
    /**
     * Extract client IP address from request, considering proxy headers.
     */
    private String extractClientIp(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("X-Real-IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("Proxy-Client-IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("WL-Proxy-Client-IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getRemoteAddr();
        }
        
        // Handle multiple IPs in X-Forwarded-For header
        if (clientIp != null && clientIp.contains(",")) {
            clientIp = clientIp.split(",")[0].trim();
        }
        
        return clientIp;
    }
}

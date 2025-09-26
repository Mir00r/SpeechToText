package com.speechtotext.api.exception;

/**
 * Exception for external service communication errors.
 */
public class ExternalServiceException extends TranscriptionException {
    
    private final String serviceName;
    
    public ExternalServiceException(String serviceName, String message) {
        super("EXTERNAL_SERVICE_ERROR", 
              String.format("Service %s error: %s", serviceName, message),
              "External service temporarily unavailable. Please try again later.");
        this.serviceName = serviceName;
    }
    
    public ExternalServiceException(String serviceName, String message, Throwable cause) {
        super("EXTERNAL_SERVICE_ERROR", 
              String.format("Service %s error: %s", serviceName, message),
              "External service temporarily unavailable. Please try again later.", 
              cause);
        this.serviceName = serviceName;
    }
    
    public ExternalServiceException(String serviceName, String message, String userMessage) {
        super("EXTERNAL_SERVICE_ERROR", 
              String.format("Service %s error: %s", serviceName, message), 
              userMessage);
        this.serviceName = serviceName;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    // Specific external service error types
    public static class TranscriptionServiceException extends ExternalServiceException {
        public TranscriptionServiceException(String message) {
            super("transcription-service", message, 
                  "The transcription service is currently unavailable. Please try again later.");
        }
        
        public TranscriptionServiceException(String message, Throwable cause) {
            super("transcription-service", message, cause);
        }
    }
    
    public static class ServiceTimeoutException extends ExternalServiceException {
        public ServiceTimeoutException(String serviceName, int timeoutSeconds) {
            super(serviceName, 
                  String.format("Service call timed out after %d seconds", timeoutSeconds),
                  "The service is taking longer than expected to respond. Please try again.");
        }
    }
    
    public static class ServiceUnavailableException extends ExternalServiceException {
        public ServiceUnavailableException(String serviceName) {
            super(serviceName, 
                  "Service is currently unavailable",
                  "The service is temporarily down for maintenance. Please try again later.");
        }
    }
    
    public static class ServiceAuthenticationException extends ExternalServiceException {
        public ServiceAuthenticationException(String serviceName) {
            super("SERVICE_AUTHENTICATION_ERROR",
                  String.format("Authentication failed for service: %s", serviceName),
                  "Service authentication failed. Please contact support.");
        }
    }
    
    public static class ServiceRateLimitException extends ExternalServiceException {
        public ServiceRateLimitException(String serviceName) {
            super("SERVICE_RATE_LIMIT",
                  String.format("Rate limit exceeded for service: %s", serviceName),
                  "Too many requests. Please wait a moment and try again.");
        }
    }
    
    public static class InvalidServiceResponseException extends ExternalServiceException {
        public InvalidServiceResponseException(String serviceName, String responseDetails) {
            super(serviceName, 
                  String.format("Invalid response received: %s", responseDetails),
                  "Received invalid response from service. Please try again or contact support.");
        }
    }
}

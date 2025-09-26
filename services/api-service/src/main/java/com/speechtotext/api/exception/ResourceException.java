package com.speechtotext.api.exception;

/**
 * Exception for configuration and resource-related errors.
 */
public class ResourceException extends TranscriptionException {
    
    public ResourceException(String message) {
        super("RESOURCE_ERROR", message, "A system resource error occurred. Please contact support.");
    }
    
    public ResourceException(String message, Throwable cause) {
        super("RESOURCE_ERROR", message, "A system resource error occurred. Please contact support.", cause);
    }
    
    public ResourceException(String message, String userMessage) {
        super("RESOURCE_ERROR", message, userMessage);
    }
    
    public ResourceException(String message, String userMessage, Throwable cause) {
        super("RESOURCE_ERROR", message, userMessage, cause);
    }
    
    // Specific resource error types
    public static class DatabaseConnectionException extends ResourceException {
        public DatabaseConnectionException(Throwable cause) {
            super(
                "Failed to connect to database",
                "Database is temporarily unavailable. Please try again later.",
                cause
            );
        }
    }
    
    public static class DatabaseOperationException extends ResourceException {
        public DatabaseOperationException(String operation, Throwable cause) {
            super(
                String.format("Database operation failed: %s", operation),
                "Database operation failed. Please try again later.",
                cause
            );
        }
    }
    
    public static class ConfigurationException extends ResourceException {
        public ConfigurationException(String configProperty, String issue) {
            super(
                String.format("Configuration error for property '%s': %s", configProperty, issue),
                "System configuration error. Please contact support."
            );
        }
    }
    
    public static class InsufficientResourcesException extends ResourceException {
        public InsufficientResourcesException(String resourceType) {
            super(
                String.format("Insufficient %s resources available", resourceType),
                "System resources are currently limited. Please try again later."
            );
        }
    }
    
    public static class ResourceLockException extends ResourceException {
        public ResourceLockException(String resourceId) {
            super(
                String.format("Failed to acquire lock for resource: %s", resourceId),
                "Resource is currently being used by another operation. Please try again."
            );
        }
    }
    
    public static class TemporaryDirectoryException extends ResourceException {
        public TemporaryDirectoryException(Throwable cause) {
            super(
                "Failed to create or access temporary directory",
                "Temporary file system error. Please try again later.",
                cause
            );
        }
    }
}

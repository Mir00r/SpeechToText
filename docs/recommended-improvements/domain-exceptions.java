package com.speechtotext.api.exception;

/**
 * Base exception for transcription domain operations.
 */
public abstract class TranscriptionException extends RuntimeException {
    
    private final String errorCode;
    
    protected TranscriptionException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    protected TranscriptionException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}

/**
 * Exception for storage-related operations.
 */
public class StorageException extends TranscriptionException {
    
    public StorageException(String message) {
        super("STORAGE_ERROR", message);
    }
    
    public StorageException(String message, Throwable cause) {
        super("STORAGE_ERROR", message, cause);
    }
}

/**
 * Exception for file validation errors.
 */
public class FileValidationException extends TranscriptionException {
    
    public FileValidationException(String message) {
        super("FILE_VALIDATION_ERROR", message);
    }
}

/**
 * Exception for external service communication.
 */
public class ExternalServiceException extends TranscriptionException {
    
    public ExternalServiceException(String service, String message) {
        super("EXTERNAL_SERVICE_ERROR", String.format("Service %s error: %s", service, message));
    }
    
    public ExternalServiceException(String service, String message, Throwable cause) {
        super("EXTERNAL_SERVICE_ERROR", String.format("Service %s error: %s", service, message), cause);
    }
}

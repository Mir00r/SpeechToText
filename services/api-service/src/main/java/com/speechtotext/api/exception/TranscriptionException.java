package com.speechtotext.api.exception;

/**
 * Base exception for all transcription-related operations.
 * Provides structured error codes and messages for better error handling and debugging.
 */
public abstract class TranscriptionException extends RuntimeException {
    
    private final String errorCode;
    private final String userMessage;
    
    protected TranscriptionException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.userMessage = message;
    }
    
    protected TranscriptionException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.userMessage = message;
    }
    
    protected TranscriptionException(String errorCode, String message, String userMessage) {
        super(message);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }
    
    protected TranscriptionException(String errorCode, String message, String userMessage, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getUserMessage() {
        return userMessage;
    }
}

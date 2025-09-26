package com.speechtotext.api.exception;

import com.speechtotext.api.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.UUID;

/**
 * Enhanced global exception handler with specific error mappings.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(FileValidationException.class)
    public ResponseEntity<ErrorResponse> handleFileValidation(FileValidationException ex) {
        String requestId = UUID.randomUUID().toString();
        logger.warn("File validation error [{}]: {}", requestId, ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            "Please check the file format and size requirements",
            Instant.now(),
            requestId
        );
        
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ErrorResponse> handleStorageError(StorageException ex) {
        String requestId = UUID.randomUUID().toString();
        logger.error("Storage error [{}]: {}", requestId, ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
            ex.getErrorCode(),
            "Storage operation failed",
            "Please try again later or contact support if the problem persists",
            Instant.now(),
            requestId
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalServiceError(ExternalServiceException ex) {
        String requestId = UUID.randomUUID().toString();
        logger.error("External service error [{}]: {}", requestId, ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
            ex.getErrorCode(),
            "External service temporarily unavailable",
            "The transcription service is currently unavailable. Please try again later.",
            Instant.now(),
            requestId
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        String requestId = UUID.randomUUID().toString();
        logger.warn("File size exceeded [{}]: {}", requestId, ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            "FILE_TOO_LARGE",
            "File size exceeds maximum allowed limit",
            "Maximum file size is 100MB. Please use a smaller file or compress the audio.",
            Instant.now(),
            requestId
        );
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(Exception ex) {
        String requestId = UUID.randomUUID().toString();
        logger.error("Unexpected error [{}]: {}", requestId, ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            "Please try again later or contact support with request ID: " + requestId,
            Instant.now(),
            requestId
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

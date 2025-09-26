package com.speechtotext.api.exception;

import com.speechtotext.api.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global exception handler with comprehensive domain-specific error handling.
 * Provides structured error responses with unique request IDs for troubleshooting.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(FileValidationException.class)
    public ResponseEntity<ErrorResponse> handleFileValidation(FileValidationException ex, WebRequest request) {
        String requestId = UUID.randomUUID().toString();
        logger.warn("File validation error [{}]: {}", requestId, ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            ex.getErrorCode(),
            ex.getUserMessage(),
            ex.getMessage(),
            Instant.now().toString(),
            requestId
        );
        
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ErrorResponse> handleStorageError(StorageException ex, WebRequest request) {
        String requestId = UUID.randomUUID().toString();
        logger.error("Storage error [{}]: {}", requestId, ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
            ex.getErrorCode(),
            ex.getUserMessage(),
            ex.getMessage(),
            Instant.now().toString(),
            requestId
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalServiceError(ExternalServiceException ex, WebRequest request) {
        String requestId = UUID.randomUUID().toString();
        logger.error("External service error [{}]: {}", requestId, ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
            ex.getErrorCode(),
            ex.getUserMessage(),
            String.format("Service: %s - %s", ex.getServiceName(), ex.getMessage()),
            Instant.now().toString(),
            requestId
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }
    
    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<ErrorResponse> handleBusinessLogicError(BusinessLogicException ex, WebRequest request) {
        String requestId = UUID.randomUUID().toString();
        
        // Different log levels based on exception type
        if (ex instanceof BusinessLogicException.JobNotFoundException) {
            logger.warn("Business logic error [{}]: {}", requestId, ex.getMessage());
        } else {
            logger.error("Business logic error [{}]: {}", requestId, ex.getMessage(), ex);
        }
        
        HttpStatus status = determineHttpStatus(ex);
        
        ErrorResponse error = new ErrorResponse(
            ex.getErrorCode(),
            ex.getUserMessage(),
            ex.getMessage(),
            Instant.now().toString(),
            requestId
        );
        
        return ResponseEntity.status(status).body(error);
    }
    
    @ExceptionHandler(ResourceException.class)
    public ResponseEntity<ErrorResponse> handleResourceError(ResourceException ex, WebRequest request) {
        String requestId = UUID.randomUUID().toString();
        logger.error("Resource error [{}]: {}", requestId, ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
            ex.getErrorCode(),
            ex.getUserMessage(),
            ex.getMessage(),
            Instant.now().toString(),
            requestId
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, WebRequest request) {
        String requestId = UUID.randomUUID().toString();
        logger.warn("File size exceeded [{}]: {}", requestId, ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            "FILE_TOO_LARGE",
            "File size exceeds maximum allowed limit",
            "Maximum file size is 100MB. Please use a smaller file or compress the audio.",
            Instant.now().toString(),
            requestId
        );
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, WebRequest request) {
        String requestId = UUID.randomUUID().toString();
        logger.warn("Validation error [{}]: {}", requestId, ex.getMessage());
        
        String validationErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining("; "));
        
        ErrorResponse error = new ErrorResponse(
            "VALIDATION_ERROR",
            "Request validation failed",
            validationErrors,
            Instant.now().toString(),
            requestId
        );
        
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseError(DataAccessException ex, WebRequest request) {
        String requestId = UUID.randomUUID().toString();
        logger.error("Database error [{}]: {}", requestId, ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
            "DATABASE_ERROR",
            "Database operation failed",
            "A database error occurred. Please try again later.",
            Instant.now().toString(),
            requestId
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(Exception ex, WebRequest request) {
        String requestId = UUID.randomUUID().toString();
        logger.error("Unexpected error [{}]: {}", requestId, ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            "Please try again later or contact support with request ID: " + requestId,
            Instant.now().toString(),
            requestId
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    /**
     * Determine appropriate HTTP status based on business logic exception type.
     */
    private HttpStatus determineHttpStatus(BusinessLogicException ex) {
        if (ex instanceof BusinessLogicException.JobNotFoundException) {
            return HttpStatus.NOT_FOUND;
        } else if (ex instanceof BusinessLogicException.InvalidJobStateException) {
            return HttpStatus.CONFLICT;
        } else if (ex instanceof BusinessLogicException.UnsupportedLanguageException ||
                   ex instanceof BusinessLogicException.UnsupportedModelException) {
            return HttpStatus.BAD_REQUEST;
        } else if (ex instanceof BusinessLogicException.ConcurrencyLimitException) {
            return HttpStatus.TOO_MANY_REQUESTS;
        } else if (ex instanceof BusinessLogicException.SyncTimeoutException) {
            return HttpStatus.REQUEST_TIMEOUT;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}

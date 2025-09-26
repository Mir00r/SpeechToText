# Domain-Specific Exception Implementation Guide

## Overview

This implementation provides a comprehensive, domain-specific exception hierarchy for the Speech to Text service, replacing generic `RuntimeException` usage with structured, meaningful exceptions that provide better error handling, debugging capabilities, and user experience.

## Exception Hierarchy

```
TranscriptionException (abstract base)
├── FileValidationException
│   ├── FileTooLargeException
│   ├── UnsupportedFormatException
│   ├── EmptyFileException
│   ├── CorruptedFileException
│   └── InvalidAudioFormatException
├── StorageException
│   ├── FileUploadException
│   ├── FileDownloadException
│   ├── FileNotFoundException
│   ├── StorageConnectionException
│   ├── InsufficientStorageException
│   └── PresignedUrlException
├── ExternalServiceException
│   ├── TranscriptionServiceException
│   ├── ServiceTimeoutException
│   ├── ServiceUnavailableException
│   ├── ServiceAuthenticationException
│   ├── ServiceRateLimitException
│   └── InvalidServiceResponseException
├── BusinessLogicException
│   ├── JobNotFoundException
│   ├── JobAlreadyProcessedException
│   ├── InvalidJobStateException
│   ├── TranscriptionFailedException
│   ├── UnsupportedLanguageException
│   ├── UnsupportedModelException
│   ├── SyncTimeoutException
│   ├── ConcurrencyLimitException
│   └── InvalidCallbackException
└── ResourceException
    ├── DatabaseConnectionException
    ├── DatabaseOperationException
    ├── ConfigurationException
    ├── InsufficientResourcesException
    ├── ResourceLockException
    └── TemporaryDirectoryException
```

## Key Features

### 1. Structured Error Information
- **Error Codes**: Consistent, machine-readable error codes
- **User Messages**: Human-friendly messages for end users
- **Technical Messages**: Detailed technical information for developers
- **Request IDs**: Unique identifiers for troubleshooting

### 2. HTTP Status Code Mapping
The `GlobalExceptionHandler` automatically maps exceptions to appropriate HTTP status codes:

- `FileValidationException` → `400 Bad Request`
- `StorageException` → `500 Internal Server Error`
- `ExternalServiceException` → `503 Service Unavailable`
- `BusinessLogicException.JobNotFoundException` → `404 Not Found`
- `BusinessLogicException.ConcurrencyLimitException` → `429 Too Many Requests`
- And more...

### 3. Enhanced Logging
- Different log levels based on exception severity
- Structured logging with request IDs
- Context-aware error messages

## Usage Examples

### File Validation
```java
@Service
public class FileValidationService {
    
    public void validateFile(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileValidationException.FileTooLargeException(
                file.getSize(), MAX_FILE_SIZE);
        }
        
        if (!SUPPORTED_FORMATS.contains(file.getContentType())) {
            throw new FileValidationException.UnsupportedFormatException(
                file.getContentType());
        }
    }
}
```

### Storage Operations
```java
@Component
public class S3ClientAdapter {
    
    public String uploadFile(MultipartFile file, String filename) {
        try {
            // Upload logic here
            return storageUrl;
        } catch (Exception e) {
            throw new StorageException.FileUploadException(filename, e);
        }
    }
}
```

### External Service Communication
```java
@Component
public class TranscriptionServiceClient {
    
    public void submitJob(UUID jobId, String s3Url) {
        try {
            // Service call logic
        } catch (RestClientException e) {
            throw new ExternalServiceException.TranscriptionServiceException(
                "Failed to communicate with transcription service", e);
        }
    }
}
```

### Business Logic Validation
```java
@Service
public class TranscriptionService {
    
    public void processCallback(UUID jobId, TranscriptionResult result) {
        JobEntity job = jobRepository.findById(jobId)
            .orElseThrow(() -> new BusinessLogicException.JobNotFoundException(jobId));
            
        if (job.getStatus() != JobStatus.PROCESSING) {
            throw new BusinessLogicException.InvalidJobStateException(
                jobId, job.getStatus().toString(), "PROCESSING");
        }
    }
}
```

## Error Response Format

All exceptions are handled by the `GlobalExceptionHandler` and return structured error responses:

```json
{
  "code": "FILE_VALIDATION_ERROR",
  "message": "File size 150.0 MB exceeds the maximum limit of 100.0 MB",
  "details": "File size 157286400 bytes exceeds maximum allowed 104857600 bytes",
  "timestamp": "2025-09-26T10:00:00Z",
  "requestId": "req_abc123xyz"
}
```

## Benefits

### 1. Better User Experience
- Clear, actionable error messages
- Consistent error format across all endpoints
- Helpful guidance for resolving issues

### 2. Improved Debugging
- Unique request IDs for tracking issues
- Structured error codes for monitoring
- Detailed technical information for developers

### 3. Enhanced Monitoring
- Categorized error metrics by exception type
- Better alerting capabilities
- Improved error rate tracking

### 4. Maintainable Code
- Clear separation of error types
- Consistent error handling patterns
- Easy to extend with new exception types

## Migration Guide

### From Generic RuntimeException
**Before:**
```java
if (file.getSize() > MAX_SIZE) {
    throw new RuntimeException("File too large");
}
```

**After:**
```java
if (file.getSize() > MAX_SIZE) {
    throw new FileValidationException.FileTooLargeException(
        file.getSize(), MAX_SIZE);
}
```

### From Generic Error Responses
**Before:**
```java
return ResponseEntity.badRequest()
    .body("Invalid file format");
```

**After:**
```java
// Exception is automatically handled by GlobalExceptionHandler
throw new FileValidationException.UnsupportedFormatException(
    file.getContentType());
```

## Testing

The implementation includes comprehensive test cases that verify:
- Proper error codes and messages
- Exception inheritance hierarchy
- HTTP status code mapping
- Request ID generation
- Logging behavior

Run tests with:
```bash
./gradlew test --tests "*ExceptionTest*"
```

## Future Enhancements

1. **Internationalization**: Support for multiple language error messages
2. **Error Analytics**: Integration with error tracking services
3. **Recovery Suggestions**: Automated recovery suggestions in error responses
4. **Rate Limiting**: Exception-based rate limiting for different error types

## Conclusion

This domain-specific exception implementation provides a robust foundation for error handling in the Speech to Text service, improving both developer experience and end-user experience while maintaining clean architecture principles.

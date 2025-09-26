package com.speechtotext.api.exception;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for domain-specific exceptions to ensure proper error codes and messages.
 */
class ExceptionTest {

    @Test
    void testFileValidationExceptions() {
        // Test FileTooLargeException
        FileValidationException.FileTooLargeException fileTooLarge = 
            new FileValidationException.FileTooLargeException(150_000_000L, 100_000_000L);
        assertEquals("FILE_VALIDATION_ERROR", fileTooLarge.getErrorCode());
        assertTrue(fileTooLarge.getMessage().contains("150000000 bytes exceeds maximum allowed 100000000 bytes"));
        assertTrue(fileTooLarge.getUserMessage().contains("143.1 MB exceeds the maximum limit of 95.4 MB"));

        // Test UnsupportedFormatException
        FileValidationException.UnsupportedFormatException unsupportedFormat = 
            new FileValidationException.UnsupportedFormatException("application/pdf");
        assertEquals("FILE_VALIDATION_ERROR", unsupportedFormat.getErrorCode());
        assertTrue(unsupportedFormat.getMessage().contains("application/pdf"));
        assertTrue(unsupportedFormat.getUserMessage().contains("Supported audio formats"));

        // Test EmptyFileException
        FileValidationException.EmptyFileException emptyFile = new FileValidationException.EmptyFileException();
        assertEquals("FILE_VALIDATION_ERROR", emptyFile.getErrorCode());
        assertTrue(emptyFile.getMessage().contains("empty"));
        assertTrue(emptyFile.getUserMessage().contains("appears to be empty"));
    }

    @Test
    void testStorageExceptions() {
        // Test FileUploadException
        Exception cause = new RuntimeException("Network timeout");
        StorageException.FileUploadException uploadException = 
            new StorageException.FileUploadException("test.wav", cause);
        assertEquals("STORAGE_ERROR", uploadException.getErrorCode());
        assertTrue(uploadException.getMessage().contains("test.wav"));
        assertEquals(cause, uploadException.getCause());

        // Test FileNotFoundException
        StorageException.FileNotFoundException notFound = 
            new StorageException.FileNotFoundException("missing-file.wav");
        assertEquals("STORAGE_ERROR", notFound.getErrorCode());
        assertTrue(notFound.getMessage().contains("missing-file.wav"));
        assertTrue(notFound.getUserMessage().contains("could not be found"));

        // Test StorageConnectionException
        StorageException.StorageConnectionException connectionException = 
            new StorageException.StorageConnectionException(new RuntimeException("Connection refused"));
        assertEquals("STORAGE_ERROR", connectionException.getErrorCode());
        assertTrue(connectionException.getUserMessage().contains("Unable to connect"));
    }

    @Test
    void testExternalServiceExceptions() {
        // Test TranscriptionServiceException
        ExternalServiceException.TranscriptionServiceException serviceException = 
            new ExternalServiceException.TranscriptionServiceException("Service unavailable");
        assertEquals("EXTERNAL_SERVICE_ERROR", serviceException.getErrorCode());
        assertEquals("transcription-service", serviceException.getServiceName());
        assertTrue(serviceException.getMessage().contains("transcription-service"));
        assertTrue(serviceException.getUserMessage().contains("currently unavailable"));

        // Test ServiceTimeoutException
        ExternalServiceException.ServiceTimeoutException timeoutException = 
            new ExternalServiceException.ServiceTimeoutException("payment-service", 30);
        assertEquals("EXTERNAL_SERVICE_ERROR", timeoutException.getErrorCode());
        assertTrue(timeoutException.getMessage().contains("30 seconds"));
        assertTrue(timeoutException.getUserMessage().contains("longer than expected"));

        // Test ServiceRateLimitException
        ExternalServiceException.ServiceRateLimitException rateLimitException = 
            new ExternalServiceException.ServiceRateLimitException("transcription-service");
        assertEquals("SERVICE_RATE_LIMIT", rateLimitException.getErrorCode());
        assertTrue(rateLimitException.getUserMessage().contains("Too many requests"));
    }

    @Test
    void testBusinessLogicExceptions() {
        UUID jobId = UUID.randomUUID();

        // Test JobNotFoundException
        BusinessLogicException.JobNotFoundException jobNotFound = 
            new BusinessLogicException.JobNotFoundException(jobId);
        assertEquals("BUSINESS_LOGIC_ERROR", jobNotFound.getErrorCode());
        assertTrue(jobNotFound.getMessage().contains(jobId.toString()));
        assertTrue(jobNotFound.getUserMessage().contains("not found"));

        // Test InvalidJobStateException
        BusinessLogicException.InvalidJobStateException invalidState = 
            new BusinessLogicException.InvalidJobStateException(jobId, "PROCESSING", "COMPLETED");
        assertEquals("BUSINESS_LOGIC_ERROR", invalidState.getErrorCode());
        assertTrue(invalidState.getMessage().contains("PROCESSING"));
        assertTrue(invalidState.getMessage().contains("COMPLETED"));

        // Test UnsupportedLanguageException
        BusinessLogicException.UnsupportedLanguageException unsupportedLang = 
            new BusinessLogicException.UnsupportedLanguageException("xyz");
        assertEquals("BUSINESS_LOGIC_ERROR", unsupportedLang.getErrorCode());
        assertTrue(unsupportedLang.getMessage().contains("xyz"));
        assertTrue(unsupportedLang.getUserMessage().contains("not currently supported"));

        // Test SyncTimeoutException
        BusinessLogicException.SyncTimeoutException syncTimeout = 
            new BusinessLogicException.SyncTimeoutException(120);
        assertEquals("BUSINESS_LOGIC_ERROR", syncTimeout.getErrorCode());
        assertTrue(syncTimeout.getMessage().contains("120 seconds"));
        assertTrue(syncTimeout.getUserMessage().contains("asynchronously"));
    }

    @Test
    void testResourceExceptions() {
        // Test DatabaseConnectionException
        Exception dbCause = new RuntimeException("Connection refused");
        ResourceException.DatabaseConnectionException dbException = 
            new ResourceException.DatabaseConnectionException(dbCause);
        assertEquals("RESOURCE_ERROR", dbException.getErrorCode());
        assertTrue(dbException.getMessage().contains("database"));
        assertEquals(dbCause, dbException.getCause());

        // Test ConfigurationException
        ResourceException.ConfigurationException configException = 
            new ResourceException.ConfigurationException("database.url", "Invalid format");
        assertEquals("RESOURCE_ERROR", configException.getErrorCode());
        assertTrue(configException.getMessage().contains("database.url"));
        assertTrue(configException.getMessage().contains("Invalid format"));

        // Test InsufficientResourcesException
        ResourceException.InsufficientResourcesException resourcesException = 
            new ResourceException.InsufficientResourcesException("memory");
        assertEquals("RESOURCE_ERROR", resourcesException.getErrorCode());
        assertTrue(resourcesException.getMessage().contains("memory"));
        assertTrue(resourcesException.getUserMessage().contains("limited"));
    }

    @Test
    void testTranscriptionExceptionHierarchy() {
        // Test that all exceptions extend TranscriptionException
        FileValidationException fileEx = new FileValidationException("test");
        assertTrue(fileEx instanceof TranscriptionException);
        
        StorageException storageEx = new StorageException("test");
        assertTrue(storageEx instanceof TranscriptionException);
        
        ExternalServiceException serviceEx = new ExternalServiceException("service", "test");
        assertTrue(serviceEx instanceof TranscriptionException);
        
        BusinessLogicException businessEx = new BusinessLogicException("test");
        assertTrue(businessEx instanceof TranscriptionException);
        
        ResourceException resourceEx = new ResourceException("test");
        assertTrue(resourceEx instanceof TranscriptionException);
    }
}

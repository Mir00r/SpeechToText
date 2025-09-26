package com.speechtotext.api.client;

import com.speechtotext.api.exception.ExternalServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test cases for circuit breaker functionality in TranscriptionServiceClient.
 */
@ExtendWith(MockitoExtension.class)
class TranscriptionServiceClientCircuitBreakerTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private TranscriptionServiceClient client;

    @Test
    void testSubmitTranscriptionJobSuccess() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        String s3Url = "s3://bucket/file.mp3";
        
        when(restTemplate.exchange(
            eq("/transcribe"),
            any(),
            any(),
            eq(String.class)
        )).thenReturn(new ResponseEntity<>("Success", HttpStatus.OK));

        // Act & Assert
        assertDoesNotThrow(() -> {
            client.submitTranscriptionJob(jobId, s3Url, false, true);
        });
        
        verify(restTemplate, times(1)).exchange(
            eq("/transcribe"),
            any(),
            any(),
            eq(String.class)
        );
    }

    @Test
    void testSubmitTranscriptionJobFailure() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        String s3Url = "s3://bucket/file.mp3";
        
        when(restTemplate.exchange(
            eq("/transcribe"),
            any(),
            any(),
            eq(String.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // Act & Assert
        ExternalServiceException.TranscriptionServiceException exception = 
            assertThrows(ExternalServiceException.TranscriptionServiceException.class, () -> {
                client.submitTranscriptionJob(jobId, s3Url, false, true);
            });
        
        assertTrue(exception.getMessage().contains("Failed to communicate with transcription service"));
    }

    @Test
    void testSyncTranscriptionJobTimeout() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        String s3Url = "s3://bucket/file.mp3";
        
        // Simulate slow response (longer than timeout)
        when(restTemplate.exchange(
            eq("/transcribe-sync"),
            any(),
            any(),
            eq(String.class)
        )).thenAnswer(invocation -> {
            Thread.sleep(3000); // 3 second delay
            return new ResponseEntity<>("{\"status\":\"COMPLETED\"}", HttpStatus.OK);
        });

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            client.submitTranscriptionJobSync(jobId, s3Url, false, true, 1); // 1 second timeout
        });
    }

    @Test
    void testCircuitBreakerFallbackBehavior() {
        // This test would require actual circuit breaker configuration
        // For now, we test the fallback methods directly
        
        UUID jobId = UUID.randomUUID();
        String s3Url = "s3://bucket/file.mp3";
        Exception simulatedException = new RuntimeException("Service unavailable");
        
        // Test async fallback
        assertThrows(ExternalServiceException.ServiceUnavailableException.class, () -> {
            client.fallbackSubmitTranscription(jobId, s3Url, false, true, simulatedException);
        });
        
        // Test sync fallback
        assertDoesNotThrow(() -> {
            CompletableFuture<?> future = client.fallbackSubmitTranscriptionSync(
                jobId, s3Url, false, true, 120, simulatedException);
            assertNotNull(future);
        });
    }

    @Test
    void testHealthCheckSuccess() {
        // Arrange
        when(restTemplate.exchange(
            eq("/health"),
            eq(org.springframework.http.HttpMethod.GET),
            isNull(),
            eq(String.class)
        )).thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));

        // Act
        boolean isHealthy = client.isTranscriptionServiceHealthy();

        // Assert
        assertTrue(isHealthy);
    }

    @Test
    void testHealthCheckFailure() {
        // Arrange
        when(restTemplate.exchange(
            eq("/health"),
            eq(org.springframework.http.HttpMethod.GET),
            isNull(),
            eq(String.class)
        )).thenThrow(new RestClientException("Connection refused"));

        // Act
        boolean isHealthy = client.isTranscriptionServiceHealthy();

        // Assert
        assertFalse(isHealthy);
    }

    @Test
    void testHealthCheckFallback() {
        // Test fallback method directly
        Exception simulatedException = new RuntimeException("Circuit breaker open");
        
        boolean result = client.fallbackHealthCheck(simulatedException);
        
        assertFalse(result);
    }
}

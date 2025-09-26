package com.speechtotext.api.client;

import com.speechtotext.api.client.TranscriptionServiceClient.TranscriptionServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TranscriptionServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    private TranscriptionServiceClient client;
    private final String callbackBaseUrl = "http://localhost:8080";

    @BeforeEach
    void setUp() {
        client = new TranscriptionServiceClient(restTemplate, callbackBaseUrl);
    }

    @Test
    void submitTranscriptionJob_Success() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        String s3Url = "s3://bucket/file.mp3";
        ResponseEntity<String> response = new ResponseEntity<>("OK", HttpStatus.OK);

        when(restTemplate.exchange(
            eq("/transcribe"),
            eq(org.springframework.http.HttpMethod.POST),
            any(),
            eq(String.class)
        )).thenReturn(response);

        // Act & Assert
        assertDoesNotThrow(() -> {
            client.submitTranscriptionJob(jobId, s3Url, true, true);
        });

        verify(restTemplate).exchange(
            eq("/transcribe"),
            eq(org.springframework.http.HttpMethod.POST),
            any(),
            eq(String.class)
        );
    }

    @Test
    void submitTranscriptionJob_ServiceError() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        String s3Url = "s3://bucket/file.mp3";
        ResponseEntity<String> response = new ResponseEntity<>("Error", HttpStatus.INTERNAL_SERVER_ERROR);

        when(restTemplate.exchange(
            eq("/transcribe"),
            eq(org.springframework.http.HttpMethod.POST),
            any(),
            eq(String.class)
        )).thenReturn(response);

        // Act & Assert
        assertThrows(TranscriptionServiceException.class, () -> {
            client.submitTranscriptionJob(jobId, s3Url, false, false);
        });
    }

    @Test
    void submitTranscriptionJob_RestClientException() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        String s3Url = "s3://bucket/file.mp3";

        when(restTemplate.exchange(
            eq("/transcribe"),
            eq(org.springframework.http.HttpMethod.POST),
            any(),
            eq(String.class)
        )).thenThrow(new RestClientException("Connection failed"));

        // Act & Assert
        assertThrows(TranscriptionServiceException.class, () -> {
            client.submitTranscriptionJob(jobId, s3Url, false, true);
        });
    }
}

package com.speechtotext.api.service;

import com.speechtotext.api.client.TranscriptionServiceClient;
import com.speechtotext.api.dto.TranscriptionCallbackRequest;
import com.speechtotext.api.dto.TranscriptionJobResponse;
import com.speechtotext.api.dto.TranscriptionResponse;
import com.speechtotext.api.dto.TranscriptionUploadRequest;
import com.speechtotext.api.infra.s3.S3ClientAdapter;
import com.speechtotext.api.mapper.JobMapper;
import com.speechtotext.api.model.JobEntity;
import com.speechtotext.api.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TranscriptionServiceSyncAsyncTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private S3ClientAdapter s3ClientAdapter;

    @Mock
    private JobMapper jobMapper;

    @Mock
    private TranscriptionServiceClient transcriptionServiceClient;

    private TranscriptionService transcriptionService;

    @BeforeEach
    void setUp() {
        transcriptionService = new TranscriptionService(
            jobRepository, s3ClientAdapter, jobMapper, transcriptionServiceClient
        );
        
        // Use reflection to set the private fields for testing
        try {
            java.lang.reflect.Field syncThresholdField = TranscriptionService.class.getDeclaredField("syncThresholdSeconds");
            syncThresholdField.setAccessible(true);
            syncThresholdField.setInt(transcriptionService, 60);

            java.lang.reflect.Field syncTimeoutField = TranscriptionService.class.getDeclaredField("syncTimeoutSeconds");
            syncTimeoutField.setAccessible(true);
            syncTimeoutField.setInt(transcriptionService, 120);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldProcessSyncForSmallFile() {
        // Arrange
        MultipartFile smallFile = new MockMultipartFile(
            "test.wav", 
            "test.wav", 
            "audio/wav", 
            new byte[500_000] // 500KB - small file
        );

        TranscriptionUploadRequest request = new TranscriptionUploadRequest("en", null, "base", false);
        JobEntity savedJob = createMockJobEntity();
        TranscriptionResponse mockResponse = createMockTranscriptionResponse();
        TranscriptionCallbackRequest mockCallback = createMockCallback();

        when(s3ClientAdapter.uploadFile(any(), any())).thenReturn("s3://bucket/file.wav");
        when(jobRepository.save(any(JobEntity.class))).thenReturn(savedJob);
        when(transcriptionServiceClient.submitTranscriptionJobSync(any(), any(), anyBoolean(), anyBoolean(), anyInt()))
            .thenReturn(mockCallback);
        when(jobRepository.findById(any())).thenReturn(java.util.Optional.of(savedJob));
        when(jobMapper.toTranscriptionResponse(any())).thenReturn(mockResponse);

        // Act
        Object result = transcriptionService.createTranscriptionJob(smallFile, request);

        // Assert
        assertInstanceOf(TranscriptionResponse.class, result);
        verify(transcriptionServiceClient).submitTranscriptionJobSync(any(), any(), anyBoolean(), anyBoolean(), anyInt());
        verify(transcriptionServiceClient, never()).submitTranscriptionJob(any(), any(), anyBoolean(), anyBoolean());
    }

    @Test
    void shouldProcessAsyncForLargeFile() {
        // Arrange
        MultipartFile largeFile = new MockMultipartFile(
            "test.wav", 
            "test.wav", 
            "audio/wav", 
            new byte[100_000_000] // 100MB - large file
        );

        TranscriptionUploadRequest request = new TranscriptionUploadRequest("en", null, "base", false);
        JobEntity savedJob = createMockJobEntity();
        TranscriptionJobResponse mockJobResponse = createMockJobResponse();

        when(s3ClientAdapter.uploadFile(any(), any())).thenReturn("s3://bucket/file.wav");
        when(jobRepository.save(any(JobEntity.class))).thenReturn(savedJob);
        when(jobMapper.toTranscriptionJobResponse(any())).thenReturn(mockJobResponse);

        // Act
        Object result = transcriptionService.createTranscriptionJob(largeFile, request);

        // Assert
        assertInstanceOf(TranscriptionJobResponse.class, result);
        verify(transcriptionServiceClient).submitTranscriptionJob(any(), any(), anyBoolean(), anyBoolean());
        verify(transcriptionServiceClient, never()).submitTranscriptionJobSync(any(), any(), anyBoolean(), anyBoolean(), anyInt());
    }

    @Test
    void shouldRespectExplicitSyncRequest() {
        // Arrange
        MultipartFile largeFile = new MockMultipartFile(
            "test.wav", 
            "test.wav", 
            "audio/wav", 
            new byte[100_000_000] // 100MB - large file, but explicitly requesting sync
        );

        TranscriptionUploadRequest request = new TranscriptionUploadRequest("en", true, "base", false); // explicit sync=true
        JobEntity savedJob = createMockJobEntity();
        TranscriptionResponse mockResponse = createMockTranscriptionResponse();
        TranscriptionCallbackRequest mockCallback = createMockCallback();

        when(s3ClientAdapter.uploadFile(any(), any())).thenReturn("s3://bucket/file.wav");
        when(jobRepository.save(any(JobEntity.class))).thenReturn(savedJob);
        when(transcriptionServiceClient.submitTranscriptionJobSync(any(), any(), anyBoolean(), anyBoolean(), anyInt()))
            .thenReturn(mockCallback);
        when(jobRepository.findById(any())).thenReturn(java.util.Optional.of(savedJob));
        when(jobMapper.toTranscriptionResponse(any())).thenReturn(mockResponse);

        // Act
        Object result = transcriptionService.createTranscriptionJob(largeFile, request);

        // Assert
        assertInstanceOf(TranscriptionResponse.class, result);
        verify(transcriptionServiceClient).submitTranscriptionJobSync(any(), any(), anyBoolean(), anyBoolean(), anyInt());
    }

    private JobEntity createMockJobEntity() {
        return new JobEntity("test.wav", "test.wav", "s3://bucket/file.wav");
    }

    private TranscriptionResponse createMockTranscriptionResponse() {
        return mock(TranscriptionResponse.class);
    }

    private TranscriptionJobResponse createMockJobResponse() {
        return mock(TranscriptionJobResponse.class);
    }

    private TranscriptionCallbackRequest createMockCallback() {
        return new TranscriptionCallbackRequest(
            "completed",
            "Hello world",
            null,
            null,
            1500L,
            30.0,
            null,
            null,
            null
        );
    }
}

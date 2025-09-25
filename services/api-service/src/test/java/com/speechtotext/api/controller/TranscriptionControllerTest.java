package com.speechtotext.api.controller;

import com.speechtotext.api.dto.TranscriptionJobResponse;
import com.speechtotext.api.dto.TranscriptionResponse;
import com.speechtotext.api.service.TranscriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TranscriptionController.
 */
@WebMvcTest(TranscriptionController.class)
class TranscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TranscriptionService transcriptionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateTranscription_AsyncSuccess() throws Exception {
        // Arrange
        UUID jobId = UUID.randomUUID();
        TranscriptionJobResponse response = new TranscriptionJobResponse(
            jobId,
            "PENDING",
            "Transcription job created successfully",
            "/api/v1/transcriptions/" + jobId
        );

        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "test.wav", 
            "audio/wav", 
            "test audio data".getBytes()
        );

        Mockito.when(transcriptionService.createTranscriptionJob(any(), any()))
            .thenReturn(response);

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/transcriptions")
                .file(file)
                .param("language", "en")
                .param("model", "base")
                .param("sync", "false"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.id").value(jobId.toString()))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.message").value("Transcription job created successfully"));
    }

    @Test
    void testCreateTranscription_InvalidFileType() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "test.txt", 
            "text/plain", 
            "not audio data".getBytes()
        );

        Mockito.when(transcriptionService.createTranscriptionJob(any(), any()))
            .thenThrow(new IllegalArgumentException("File type 'text/plain' not supported"));

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/transcriptions")
                .file(file))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.message").value("File type 'text/plain' not supported"));
    }

    @Test
    void testGetTranscription_Success() throws Exception {
        // Arrange
        UUID jobId = UUID.randomUUID();
        TranscriptionResponse response = new TranscriptionResponse(
            jobId,
            "test.wav",
            "COMPLETED",
            "Hello world, this is a test.",
            "base",
            "en",
            1024L,
            BigDecimal.valueOf(30.5),
            LocalDateTime.now().minusMinutes(5),
            LocalDateTime.now(),
            LocalDateTime.now().minusMinutes(4),
            LocalDateTime.now(),
            null,
            "/api/v1/transcriptions/" + jobId + "/download"
        );

        Mockito.when(transcriptionService.getTranscriptionJob(jobId))
            .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/transcriptions/{id}", jobId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(jobId.toString()))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.transcript_text").value("Hello world, this is a test."));
    }

    @Test
    void testGetTranscription_NotFound() throws Exception {
        // Arrange
        UUID jobId = UUID.randomUUID();
        Mockito.when(transcriptionService.getTranscriptionJob(jobId))
            .thenThrow(new IllegalArgumentException("Job not found: " + jobId));

        // Act & Assert
        mockMvc.perform(get("/api/v1/transcriptions/{id}", jobId))
            .andExpect(status().isNotFound());
    }

    @Test
    void testDownloadTranscript_NotCompleted() throws Exception {
        // Arrange
        UUID jobId = UUID.randomUUID();
        Mockito.when(transcriptionService.downloadTranscript(jobId))
            .thenThrow(new IllegalStateException("Transcription is not completed yet"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/transcriptions/{id}/download", jobId))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("TRANSCRIPT_NOT_AVAILABLE"));
    }
}

package com.speechtotext.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.speechtotext.api.dto.TranscriptionCallbackRequest;
import com.speechtotext.api.service.TranscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InternalController.class)
class InternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TranscriptionService transcriptionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void handleTranscriptionCallback_Success() throws Exception {
        // Arrange
        UUID jobId = UUID.randomUUID();
        TranscriptionCallbackRequest request = new TranscriptionCallbackRequest(
            "completed",
            "Hello world",
            "s3://bucket/transcript.txt",
            null,
            1500L,
            10.5,
            null,
            null,
            null
        );

        doNothing().when(transcriptionService).handleTranscriptionCallback(eq(jobId), any());

        // Act & Assert
        mockMvc.perform(post("/internal/v1/transcriptions/{id}/callback", jobId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Callback processed successfully"));
    }

    @Test
    void handleTranscriptionCallback_InvalidRequest() throws Exception {
        // Arrange
        UUID jobId = UUID.randomUUID();
        TranscriptionCallbackRequest request = new TranscriptionCallbackRequest(
            null, // Invalid - status is required
            "Hello world",
            "s3://bucket/transcript.txt",
            null,
            1500L,
            10.5,
            null,
            null,
            null
        );

        // Act & Assert
        mockMvc.perform(post("/internal/v1/transcriptions/{id}/callback", jobId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}

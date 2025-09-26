package com.speechtotext.api.controller;

import com.speechtotext.api.dto.TranscriptionCallbackRequest;
import com.speechtotext.api.service.TranscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@RestController
@RequestMapping("/internal/v1")
public class InternalController {

    private static final Logger logger = LoggerFactory.getLogger(InternalController.class);

    @Autowired
    private TranscriptionService transcriptionService;

    @PostMapping("/transcriptions/{id}/callback")
    public ResponseEntity<String> handleTranscriptionCallback(
            @PathVariable UUID id,
            @Valid @RequestBody TranscriptionCallbackRequest request) {
        
        try {
            logger.info("Received transcription callback for job {}", id);
            transcriptionService.handleTranscriptionCallback(id, request);
            return ResponseEntity.ok("Callback processed successfully");
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid callback request for job {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error processing callback for job {}", id, e);
            return ResponseEntity.internalServerError().body("Internal server error");
        }
    }
}

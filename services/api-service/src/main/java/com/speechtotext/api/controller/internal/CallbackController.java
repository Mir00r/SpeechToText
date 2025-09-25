package com.speechtotext.api.controller.internal;

import com.speechtotext.api.model.JobEntity;
import com.speechtotext.api.service.TranscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Internal controller for callbacks from transcription service.
 * This endpoint should be protected in production.
 */
@RestController
@RequestMapping("/internal/v1/transcriptions")
@Tag(name = "Internal Callbacks", description = "Internal endpoints for service-to-service communication")
public class CallbackController {

    private static final Logger logger = LoggerFactory.getLogger(CallbackController.class);

    private final TranscriptionService transcriptionService;

    public CallbackController(TranscriptionService transcriptionService) {
        this.transcriptionService = transcriptionService;
    }

    @Operation(
        summary = "Update transcription job result",
        description = "Internal endpoint called by transcription service to update job status and results"
    )
    @PostMapping("/{id}/callback")
    public ResponseEntity<Void> updateTranscriptionResult(
        @PathVariable UUID id,
        @RequestBody TranscriptionCallbackRequest request
    ) {
        try {
            logger.info("Received callback for job {} with status: {}", id, request.status());

            JobEntity.JobStatus status = JobEntity.JobStatus.valueOf(request.status().toUpperCase());
            
            transcriptionService.updateJobResult(
                id, 
                status, 
                request.transcriptText(), 
                request.timestampsJson(), 
                request.errorMessage()
            );

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            logger.error("Error processing callback for job: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Request DTO for transcription callbacks.
     */
    public record TranscriptionCallbackRequest(
        String status,
        String transcriptText,
        String timestampsJson,
        String errorMessage
    ) {}
}

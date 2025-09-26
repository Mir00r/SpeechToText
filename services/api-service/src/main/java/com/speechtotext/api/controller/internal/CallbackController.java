package com.speechtotext.api.controller.internal;

import com.speechtotext.api.dto.TranscriptionCallbackRequest;
import com.speechtotext.api.service.TranscriptionService;
import com.speechtotext.api.trace.TraceContext;
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
            // Set job context for tracing
            TraceContext.setJobContext(id.toString());
            
            logger.info("Received callback for job {} with status: {} [correlationId={}]", 
                       id, request.status(), TraceContext.getCorrelationId());

            transcriptionService.handleTranscriptionCallback(id, request);

            logger.info("Successfully processed callback for job {} [correlationId={}]", 
                       id, TraceContext.getCorrelationId());

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            logger.error("Error processing callback for job: {} [correlationId={}]", 
                        id, TraceContext.getCorrelationId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

}

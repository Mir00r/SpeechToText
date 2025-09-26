package com.speechtotext.api.controller;

import com.speechtotext.api.dto.ErrorResponse;
import com.speechtotext.api.dto.TranscriptionJobResponse;
import com.speechtotext.api.dto.TranscriptionResponse;
import com.speechtotext.api.dto.TranscriptionUploadRequest;
import com.speechtotext.api.service.TranscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

/**
 * REST controller for transcription operations.
 */
@RestController
@RequestMapping("/api/v1/transcriptions")
@Tag(name = "Transcriptions", description = "Audio transcription operations")
public class TranscriptionController {

    private static final Logger logger = LoggerFactory.getLogger(TranscriptionController.class);

    private final TranscriptionService transcriptionService;

    public TranscriptionController(TranscriptionService transcriptionService) {
        this.transcriptionService = transcriptionService;
    }

    @Operation(
        summary = "Create a new transcription job",
        description = "Upload an audio file for transcription. Returns job ID for async processing or transcript for sync processing."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sync transcription completed", 
                    content = @Content(schema = @Schema(implementation = TranscriptionResponse.class))),
        @ApiResponse(responseCode = "202", description = "Async transcription job created",
                    content = @Content(schema = @Schema(implementation = TranscriptionJobResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid file or parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "413", description = "File too large",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createTranscription(
        @Parameter(description = "Audio file to transcribe", required = true)
        @RequestParam("file") MultipartFile file,
        
        @Parameter(description = "Language code (ISO 639-1) or 'auto' for automatic detection")
        @RequestParam(value = "language", defaultValue = "auto") String language,
        
        @Parameter(description = "Process synchronously if file is small enough")
        @RequestParam(value = "sync", defaultValue = "false") Boolean sync,
        
        @Parameter(description = "Whisper model to use ('auto' for intelligent selection)")
        @RequestParam(value = "model", defaultValue = "auto") String model,
        
        @Parameter(description = "Enable speaker diarization")
        @RequestParam(value = "diarize", defaultValue = "false") Boolean diarize,
        
        @Parameter(description = "Quality preference for transcription")
        @RequestParam(value = "quality", defaultValue = "balanced") String quality
    ) {
        try {
            logger.info("Received transcription request for file: {} (size: {} bytes)", 
                       file.getOriginalFilename(), file.getSize());

            TranscriptionUploadRequest request = new TranscriptionUploadRequest(language, sync, model, diarize, quality);
            
            Object result = transcriptionService.createTranscriptionJob(file, request);

            if (result instanceof TranscriptionResponse) {
                // Sync processing completed
                return ResponseEntity.ok(result);
            } else {
                // Async job created
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
            }

        } catch (IllegalArgumentException e) {
            logger.warn("Bad request for transcription: {}", e.getMessage());
            ErrorResponse error = new ErrorResponse(
                "INVALID_REQUEST", 
                e.getMessage(), 
                null,
                Instant.now().toString()
            );
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            logger.error("Internal error during transcription request", e);
            ErrorResponse error = new ErrorResponse(
                "INTERNAL_ERROR", 
                "An unexpected error occurred", 
                e.getMessage(),
                Instant.now().toString()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @Operation(
        summary = "Get transcription job status",
        description = "Retrieve the status and result of a transcription job"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Job found",
                    content = @Content(schema = @Schema(implementation = TranscriptionResponse.class))),
        @ApiResponse(responseCode = "404", description = "Job not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getTranscription(
        @Parameter(description = "Job ID", required = true)
        @PathVariable UUID id
    ) {
        try {
            logger.debug("Getting transcription job: {}", id);
            
            TranscriptionResponse response = transcriptionService.getTranscriptionJob(id);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Job not found: {}", id);
            ErrorResponse error = new ErrorResponse(
                "JOB_NOT_FOUND", 
                "Transcription job not found", 
                "Job ID: " + id,
                Instant.now().toString()
            );
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            logger.error("Error retrieving transcription job: {}", id, e);
            ErrorResponse error = new ErrorResponse(
                "INTERNAL_ERROR", 
                "An unexpected error occurred", 
                e.getMessage(),
                Instant.now().toString()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @Operation(
        summary = "Download transcript",
        description = "Download the transcript as a plain text file"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transcript file",
                    content = @Content(mediaType = "text/plain")),
        @ApiResponse(responseCode = "404", description = "Job not found or not completed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadTranscript(
        @Parameter(description = "Job ID", required = true)
        @PathVariable UUID id
    ) {
        try {
            logger.debug("Downloading transcript for job: {}", id);
            
            Resource resource = transcriptionService.downloadTranscript(id);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transcript_" + id + ".txt\"");
            headers.add(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(resource);

        } catch (IllegalArgumentException e) {
            logger.warn("Job not found for download: {}", id);
            ErrorResponse error = new ErrorResponse(
                "JOB_NOT_FOUND", 
                "Transcription job not found", 
                "Job ID: " + id,
                Instant.now().toString()
            );
            return ResponseEntity.notFound().build();

        } catch (IllegalStateException e) {
            logger.warn("Transcript not available for job: {}", id);
            ErrorResponse error = new ErrorResponse(
                "TRANSCRIPT_NOT_AVAILABLE", 
                e.getMessage(), 
                "Job ID: " + id,
                Instant.now().toString()
            );
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            logger.error("Error downloading transcript for job: {}", id, e);
            ErrorResponse error = new ErrorResponse(
                "INTERNAL_ERROR", 
                "An unexpected error occurred", 
                e.getMessage(),
                Instant.now().toString()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

package com.speechtotext.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Response DTO for successful job creation (async processing).
 */
@Schema(description = "Response when a transcription job is created for async processing")
public record TranscriptionJobResponse(
    
    @Schema(description = "Unique job identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,
    
    @Schema(description = "Job status", example = "PENDING")
    String status,
    
    @Schema(description = "Message describing the job status", example = "Transcription job created successfully")
    String message,
    
    @Schema(description = "URL to poll for job status", example = "/api/v1/transcriptions/550e8400-e29b-41d4-a716-446655440000")
    @JsonProperty("status_url")
    String statusUrl
) {}

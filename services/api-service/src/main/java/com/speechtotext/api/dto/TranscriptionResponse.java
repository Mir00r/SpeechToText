package com.speechtotext.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for transcription job information.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Transcription job information")
public record TranscriptionResponse(
    
    @Schema(description = "Unique job identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,
    
    @Schema(description = "Original filename", example = "recording.wav")
    @JsonProperty("original_filename")
    String originalFilename,
    
    @Schema(description = "Current job status", example = "COMPLETED")
    String status,
    
    @Schema(description = "Transcribed text", example = "Hello world, this is a test recording.")
    @JsonProperty("transcript_text")
    String transcriptText,
    
    @Schema(description = "Whisper model used", example = "base")
    String model,
    
    @Schema(description = "Language detected/specified", example = "en")
    String language,
    
    @Schema(description = "File size in bytes", example = "1024000")
    @JsonProperty("file_size_bytes")
    Long fileSizeBytes,
    
    @Schema(description = "Audio duration in seconds", example = "45.67")
    @JsonProperty("duration_seconds")
    BigDecimal durationSeconds,
    
    @Schema(description = "Job creation timestamp", example = "2023-12-01T10:00:00")
    @JsonProperty("created_at")
    LocalDateTime createdAt,
    
    @Schema(description = "Job last update timestamp", example = "2023-12-01T10:05:30")
    @JsonProperty("updated_at")
    LocalDateTime updatedAt,
    
    @Schema(description = "Processing start timestamp", example = "2023-12-01T10:01:00")
    @JsonProperty("started_at")
    LocalDateTime startedAt,
    
    @Schema(description = "Processing completion timestamp", example = "2023-12-01T10:05:30")
    @JsonProperty("finished_at")
    LocalDateTime finishedAt,
    
    @Schema(description = "Error message if failed", example = "Audio format not supported")
    @JsonProperty("error_message")
    String errorMessage,
    
    @Schema(description = "Download URL for transcript", example = "/api/v1/transcriptions/550e8400-e29b-41d4-a716-446655440000/download")
    @JsonProperty("download_url")
    String downloadUrl
) {}

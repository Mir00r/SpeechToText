package com.speechtotext.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Standard error response DTO.
 */
@Schema(description = "Error response")
public record ErrorResponse(
    
    @Schema(description = "Error code", example = "INVALID_FILE_FORMAT")
    String code,
    
    @Schema(description = "Error message", example = "File format not supported. Please upload WAV, MP3, M4A, or FLAC files.")
    String message,
    
    @Schema(description = "Additional error details", example = "Uploaded file has MIME type: application/pdf")
    String details,
    
    @Schema(description = "Timestamp of error", example = "2023-12-01T10:00:00Z")
    String timestamp
) {}

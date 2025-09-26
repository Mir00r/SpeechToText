package com.speechtotext.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for transcription upload.
 */
@Schema(description = "Request for creating a new transcription job")
public record TranscriptionUploadRequest(
    
    @Schema(description = "Audio language code (ISO 639-1)", 
            example = "en", 
            defaultValue = "auto")
    @Size(max = 10, message = "Language code must be at most 10 characters")
    @Pattern(regexp = "^(auto|[a-z]{2})$", message = "Language must be 'auto' or a valid 2-letter ISO code")
    String language,
    
    @Schema(description = "Process synchronously if file is small", 
            example = "false",
            defaultValue = "false") 
    @JsonProperty("sync")
    Boolean synchronous,
    
    @Schema(description = "Whisper model to use ('auto' for intelligent selection)", 
            example = "base",
            defaultValue = "auto",
            allowableValues = {"auto", "tiny", "base", "small", "medium", "large"})
    @Pattern(regexp = "^(auto|tiny|base|small|medium|large)$", message = "Model must be 'auto' or one of: tiny, base, small, medium, large")
    String model,
    
    @Schema(description = "Enable speaker diarization", 
            example = "false",
            defaultValue = "false")
    Boolean diarize,
    
    @Schema(description = "Quality preference for transcription ('speed', 'balanced', 'accuracy', 'precision')", 
            example = "balanced",
            defaultValue = "balanced",
            allowableValues = {"speed", "balanced", "accuracy", "precision"})
    @Pattern(regexp = "^(speed|balanced|accuracy|precision)$", message = "Quality preference must be one of: speed, balanced, accuracy, precision")
    String quality
) {

    // Constructor with defaults
    public TranscriptionUploadRequest {
        language = language != null ? language : "auto";
        synchronous = synchronous != null ? synchronous : false;
        model = model != null ? model : "auto";
        diarize = diarize != null ? diarize : false;
        quality = quality != null ? quality : "balanced";
    }

    /**
     * Static factory method for defaults.
     */
    public static TranscriptionUploadRequest withDefaults() {
        return new TranscriptionUploadRequest("auto", false, "auto", false, "balanced");
    }
}

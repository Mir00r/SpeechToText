package com.speechtotext.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

public record TranscriptionCallbackRequest(
        @NotNull
        @NotBlank
        String status,

        @JsonProperty("transcript_text")
        String transcriptText,

        @JsonProperty("s3_transcript_url")
        String s3TranscriptUrl,

        @JsonProperty("error_message")
        String errorMessage,

        @JsonProperty("processing_duration_ms")
        Long processingDurationMs,

        @JsonProperty("audio_duration_s")
        Double audioDurationS,

        // Detailed transcription results
        List<TranscriptionSegment> segments,

        // Speaker diarization results (if enabled)
        @JsonProperty("speaker_segments")
        List<SpeakerSegment> speakerSegments,

        // Additional metadata
        Map<String, Object> metadata
) {
    public record TranscriptionSegment(
            Double start,
            Double end,
            String text,
            List<WordAlignment> words
    ) {}

    public record WordAlignment(
            String word,
            Double start,
            Double end,
            Double score
    ) {}

    public record SpeakerSegment(
            String speaker,
            Double start,
            Double end
    ) {}
}

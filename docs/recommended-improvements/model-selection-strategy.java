package com.speechtotext.api.strategy;

/**
 * Strategy for selecting optimal Whisper model based on audio characteristics.
 */
public interface ModelSelectionStrategy {
    String selectModel(AudioMetadata metadata, QualityPreference preference);
}

/**
 * Audio file metadata for model selection.
 */
public class AudioMetadata {
    private final long fileSizeBytes;
    private final double estimatedDurationSeconds;
    private final String language;
    
    public AudioMetadata(long fileSizeBytes, double estimatedDurationSeconds, String language) {
        this.fileSizeBytes = fileSizeBytes;
        this.estimatedDurationSeconds = estimatedDurationSeconds;
        this.language = language;
    }
    
    // Getters
    public long getFileSizeBytes() { return fileSizeBytes; }
    public double getEstimatedDurationSeconds() { return estimatedDurationSeconds; }
    public String getLanguage() { return language; }
}

/**
 * Quality preference for transcription.
 */
public enum QualityPreference {
    SPEED,      // Fastest processing
    BALANCED,   // Balance of speed and accuracy  
    ACCURACY    // Highest accuracy
}

/**
 * Default implementation of model selection strategy.
 */
@Component
public class DefaultModelSelectionStrategy implements ModelSelectionStrategy {
    
    private static final long SMALL_FILE_THRESHOLD = 5 * 1024 * 1024; // 5MB
    private static final long LARGE_FILE_THRESHOLD = 50 * 1024 * 1024; // 50MB
    private static final double SHORT_DURATION_THRESHOLD = 60.0; // 1 minute
    private static final double LONG_DURATION_THRESHOLD = 3600.0; // 1 hour
    
    @Override
    public String selectModel(AudioMetadata metadata, QualityPreference preference) {
        
        // For very short files, use base model for speed
        if (metadata.getEstimatedDurationSeconds() < SHORT_DURATION_THRESHOLD) {
            return "base";
        }
        
        // For very long files, consider memory constraints
        if (metadata.getEstimatedDurationSeconds() > LONG_DURATION_THRESHOLD || 
            metadata.getFileSizeBytes() > LARGE_FILE_THRESHOLD) {
            return preference == QualityPreference.ACCURACY ? "small" : "base";
        }
        
        // Default selection based on quality preference
        return switch (preference) {
            case SPEED -> "base";
            case BALANCED -> "small";
            case ACCURACY -> "medium";
        };
    }
}

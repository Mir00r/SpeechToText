package com.speechtotext.api.strategy;

/**
 * Strategy interface for selecting optimal Whisper model based on audio characteristics.
 * 
 * This strategy pattern allows for flexible model selection algorithms that can be
 * swapped out based on requirements, performance characteristics, or user preferences.
 */
public interface ModelSelectionStrategy {
    
    /**
     * Select the optimal Whisper model based on audio metadata and quality preference.
     * 
     * @param metadata Audio file metadata including size, duration, and language
     * @param preference Quality preference indicating the trade-off between speed and accuracy
     * @return The selected Whisper model name (tiny, base, small, medium, large)
     */
    String selectModel(AudioMetadata metadata, QualityPreference preference);
    
    /**
     * Get the strategy name for logging and debugging purposes.
     * 
     * @return Human-readable strategy name
     */
    default String getStrategyName() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * Validate if the strategy can handle the given audio metadata.
     * 
     * @param metadata Audio metadata to validate
     * @return true if the strategy can process this metadata, false otherwise
     */
    default boolean canHandle(AudioMetadata metadata) {
        return metadata != null && 
               metadata.getFileSizeBytes() > 0 && 
               metadata.getEstimatedDurationSeconds() > 0;
    }
}

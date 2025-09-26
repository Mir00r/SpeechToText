package com.speechtotext.api.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Accuracy-focused model selection strategy.
 * 
 * Prioritizes transcription accuracy over processing speed and resource usage.
 * Suitable for critical applications, legal documentation, medical transcription,
 * or any scenario where accuracy is paramount.
 */
@Component
@ConditionalOnProperty(name = "app.model-selection.strategy", havingValue = "accuracy")
public class AccuracyFocusedStrategy implements ModelSelectionStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(AccuracyFocusedStrategy.class);
    
    // Thresholds for accuracy-focused selection
    private static final long LARGE_MODEL_MIN_SIZE = 50 * 1024 * 1024; // 50MB
    private static final double LARGE_MODEL_MAX_DURATION = 2400.0; // 40 minutes
    
    @Override
    public String selectModel(AudioMetadata metadata, QualityPreference preference) {
        if (!canHandle(metadata)) {
            logger.warn("Cannot handle metadata with accuracy strategy, falling back to medium");
            return "medium";
        }
        
        String selectedModel = performSelection(metadata, preference);
        logger.debug("Accuracy strategy selected '{}' for audio: {}", selectedModel, metadata);
        
        return selectedModel;
    }
    
    private String performSelection(AudioMetadata metadata, QualityPreference preference) {
        // For speed preference, still use better models than default strategy
        if (preference == QualityPreference.SPEED) {
            return metadata.isSmallFile() ? "base" : "small";
        }
        
        // For complex languages, always prefer larger models
        if (metadata.requiresLargerModel()) {
            return selectForComplexLanguage(metadata, preference);
        }
        
        // For very long files, balance accuracy with practicality
        if (metadata.getEstimatedDurationSeconds() > 3600.0) { // > 1 hour
            return preference.prioritizesAccuracy() ? "medium" : "small";
        }
        
        // For large files, use large model if feasible
        if (metadata.getFileSizeBytes() > LARGE_MODEL_MIN_SIZE && 
            metadata.getEstimatedDurationSeconds() <= LARGE_MODEL_MAX_DURATION) {
            return "large";
        }
        
        // Default accuracy-focused selection
        return switch (preference) {
            case SPEED -> "small";
            case BALANCED -> "medium";
            case ACCURACY, PRECISION -> "large";
        };
    }
    
    private String selectForComplexLanguage(AudioMetadata metadata, QualityPreference preference) {
        // For complex languages, bias towards larger models
        if (metadata.isLargeFile() || metadata.isLongFile()) {
            // Cap at medium for very large content
            return switch (preference) {
                case SPEED -> "small";
                case BALANCED -> "medium";
                case ACCURACY, PRECISION -> "medium";
            };
        }
        
        // For smaller complex language content, use largest feasible model
        return switch (preference) {
            case SPEED -> "medium";
            case BALANCED -> "large";
            case ACCURACY, PRECISION -> "large";
        };
    }
    
    @Override
    public String getStrategyName() {
        return "AccuracyFocused";
    }
    
    @Override
    public boolean canHandle(AudioMetadata metadata) {
        return metadata != null && 
               metadata.getFileSizeBytes() > 0 && 
               metadata.getEstimatedDurationSeconds() > 0 &&
               metadata.getEstimatedDurationSeconds() < 7200.0; // Cap at 2 hours
    }
}

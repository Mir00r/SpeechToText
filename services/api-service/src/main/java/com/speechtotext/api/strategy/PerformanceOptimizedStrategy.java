package com.speechtotext.api.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Performance-optimized model selection strategy.
 * 
 * Prioritizes processing speed and resource efficiency over transcription accuracy.
 * Suitable for high-throughput scenarios, real-time applications, or when accuracy
 * requirements are less stringent.
 */
@Component
@ConditionalOnProperty(name = "app.model-selection.strategy", havingValue = "performance")
public class PerformanceOptimizedStrategy implements ModelSelectionStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceOptimizedStrategy.class);
    
    // Conservative thresholds for performance optimization
    private static final long TINY_MODEL_MAX_SIZE = 10 * 1024 * 1024; // 10MB
    private static final double TINY_MODEL_MAX_DURATION = 300.0; // 5 minutes
    private static final double BASE_MODEL_MAX_DURATION = 1200.0; // 20 minutes
    
    @Override
    public String selectModel(AudioMetadata metadata, QualityPreference preference) {
        if (!canHandle(metadata)) {
            logger.warn("Cannot handle metadata with performance strategy, falling back to base");
            return "base";
        }
        
        String selectedModel = performSelection(metadata, preference);
        logger.debug("Performance strategy selected '{}' for audio: {}", selectedModel, metadata);
        
        return selectedModel;
    }
    
    private String performSelection(AudioMetadata metadata, QualityPreference preference) {
        // For very small files, use tiny model for maximum speed
        if (metadata.getFileSizeBytes() <= TINY_MODEL_MAX_SIZE && 
            metadata.getEstimatedDurationSeconds() <= TINY_MODEL_MAX_DURATION) {
            return "tiny";
        }
        
        // For short to medium files, use base model
        if (metadata.getEstimatedDurationSeconds() <= BASE_MODEL_MAX_DURATION) {
            // Only upgrade to small if accuracy is explicitly requested
            if (preference.prioritizesAccuracy() && !metadata.isLargeFile()) {
                return "small";
            }
            return "base";
        }
        
        // For longer files, stay with base model unless accuracy is critical
        // and the file isn't too large
        if (preference == QualityPreference.PRECISION && 
            !metadata.isLargeFile() && 
            metadata.getEstimatedDurationSeconds() < 3600.0) { // < 1 hour
            return "small";
        }
        
        return "base"; // Default to base for performance
    }
    
    @Override
    public String getStrategyName() {
        return "PerformanceOptimized";
    }
    
    @Override
    public boolean canHandle(AudioMetadata metadata) {
        return metadata != null && 
               metadata.getFileSizeBytes() > 0 && 
               metadata.getEstimatedDurationSeconds() > 0 &&
               metadata.getEstimatedDurationSeconds() < 3600.0; // Cap at 1 hour for performance
    }
}

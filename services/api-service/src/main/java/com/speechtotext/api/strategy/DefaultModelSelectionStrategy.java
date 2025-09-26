package com.speechtotext.api.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Default implementation of model selection strategy.
 * 
 * Uses a sophisticated algorithm that considers file size, duration, language complexity,
 * and quality preference to select the optimal Whisper model for transcription.
 */
@Component
public class DefaultModelSelectionStrategy implements ModelSelectionStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultModelSelectionStrategy.class);
    
    // Thresholds for file classification
    private static final long SMALL_FILE_THRESHOLD = 5 * 1024 * 1024; // 5MB
    private static final long MEDIUM_FILE_THRESHOLD = 25 * 1024 * 1024; // 25MB  
    private static final long LARGE_FILE_THRESHOLD = 50 * 1024 * 1024; // 50MB
    
    private static final double SHORT_DURATION_THRESHOLD = 120.0; // 2 minutes
    private static final double MEDIUM_DURATION_THRESHOLD = 900.0; // 15 minutes
    private static final double LONG_DURATION_THRESHOLD = 1800.0; // 30 minutes
    
    // Model performance characteristics (relative processing speed)
    private static final double TINY_SPEED = 10.0;
    private static final double BASE_SPEED = 5.0;
    private static final double SMALL_SPEED = 3.0;
    private static final double MEDIUM_SPEED = 2.0;
    private static final double LARGE_SPEED = 1.0;
    
    @Value("${app.model-selection.enable-dynamic:true}")
    private boolean enableDynamicSelection;
    
    @Value("${app.model-selection.fallback-model:base}")
    private String fallbackModel;
    
    @Value("${app.model-selection.max-processing-time-minutes:30}")
    private int maxProcessingTimeMinutes;
    
    @Override
    public String selectModel(AudioMetadata metadata, QualityPreference preference) {
        if (!enableDynamicSelection) {
            logger.debug("Dynamic model selection disabled, using fallback: {}", fallbackModel);
            return fallbackModel;
        }
        
        if (!canHandle(metadata)) {
            logger.warn("Invalid metadata provided, using fallback model: {}", fallbackModel);
            return fallbackModel;
        }
        
        String selectedModel = performSelection(metadata, preference);
        logger.info("Selected model '{}' for audio: {} with preference: {}", 
                   selectedModel, metadata, preference);
        
        return selectedModel;
    }
    
    /**
     * Core model selection logic.
     */
    private String performSelection(AudioMetadata metadata, QualityPreference preference) {
        // Quick selection for very short files - prioritize speed
        if (metadata.isShortFile() && metadata.isSmallFile()) {
            return switch (preference) {
                case SPEED -> "tiny";
                case BALANCED -> "base";
                case ACCURACY, PRECISION -> "small";
            };
        }
        
        // For very long files or large files, consider memory constraints
        if (metadata.isLongFile() || metadata.isLargeFile()) {
            return selectForLargeContent(metadata, preference);
        }
        
        // Consider language complexity
        if (metadata.requiresLargerModel()) {
            return selectForComplexLanguage(metadata, preference);
        }
        
        // Estimate processing time and select accordingly
        return selectByProcessingTimeEstimate(metadata, preference);
    }
    
    /**
     * Select model for large content (long duration or large file size).
     */
    private String selectForLargeContent(AudioMetadata metadata, QualityPreference preference) {
        // For extremely large files, prioritize completion over accuracy
        if (metadata.getFileSizeBytes() > 100 * 1024 * 1024 || // > 100MB
            metadata.getEstimatedDurationSeconds() > 3600.0) {    // > 1 hour
            
            return switch (preference) {
                case SPEED -> "tiny";
                case BALANCED -> "base";
                case ACCURACY -> "small";
                case PRECISION -> "medium"; // Still cap at medium for very large files
            };
        }
        
        // For moderately large files
        return switch (preference) {
            case SPEED -> "base";
            case BALANCED -> "small";
            case ACCURACY -> "medium";
            case PRECISION -> "large";
        };
    }
    
    /**
     * Select model for complex languages that benefit from larger models.
     */
    private String selectForComplexLanguage(AudioMetadata metadata, QualityPreference preference) {
        // Boost model size by one level for complex languages
        String baseModel = switch (preference) {
            case SPEED -> "base";        // tiny -> base
            case BALANCED -> "small";    // base -> small  
            case ACCURACY -> "medium";   // small -> medium
            case PRECISION -> "large";   // medium -> large
        };
        
        // But still respect file size constraints for very large files
        if (metadata.isLargeFile()) {
            return limitModelForLargeFile(baseModel);
        }
        
        return baseModel;
    }
    
    /**
     * Select model based on estimated processing time.
     */
    private String selectByProcessingTimeEstimate(AudioMetadata metadata, QualityPreference preference) {
        double maxAcceptableMinutes = maxProcessingTimeMinutes * preference.getProcessingTimeMultiplier();
        
        // Estimate processing time for each model (very rough approximation)
        double durationMinutes = metadata.getEstimatedDurationSeconds() / 60.0;
        
        // Tiny: ~1x duration, Base: ~2x, Small: ~3x, Medium: ~5x, Large: ~10x
        if (durationMinutes * 10.0 <= maxAcceptableMinutes && preference.prioritizesAccuracy()) {
            return "large";
        } else if (durationMinutes * 5.0 <= maxAcceptableMinutes && 
                   (preference.prioritizesAccuracy() || preference.isBalanced())) {
            return "medium";
        } else if (durationMinutes * 3.0 <= maxAcceptableMinutes) {
            return "small";
        } else if (durationMinutes * 2.0 <= maxAcceptableMinutes) {
            return "base";
        } else {
            return "tiny";
        }
    }
    
    /**
     * Limit model size for large files to prevent memory issues.
     */
    private String limitModelForLargeFile(String model) {
        return switch (model) {
            case "large" -> "medium";
            case "medium" -> "small";
            default -> model;
        };
    }
    
    @Override
    public String getStrategyName() {
        return "DefaultModelSelection";
    }
    
    @Override
    public boolean canHandle(AudioMetadata metadata) {
        return metadata != null && 
               metadata.getFileSizeBytes() > 0 && 
               metadata.getEstimatedDurationSeconds() > 0 &&
               metadata.getEstimatedDurationSeconds() < 7200.0; // Cap at 2 hours
    }
}

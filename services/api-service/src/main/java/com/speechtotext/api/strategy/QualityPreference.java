package com.speechtotext.api.strategy;

/**
 * Quality preference enumeration for transcription model selection.
 * 
 * Represents the trade-off between processing speed, resource usage, and transcription accuracy.
 * Different preferences will influence which Whisper model is selected for optimal performance.
 */
public enum QualityPreference {
    
    /**
     * Prioritize processing speed over accuracy.
     * Suitable for real-time applications, quick previews, or when accuracy is not critical.
     * Tends to select smaller, faster models like 'tiny' or 'base'.
     */
    SPEED("speed", "Fast processing with lower accuracy", 1),
    
    /**
     * Balance between speed and accuracy.
     * Suitable for most production applications where both speed and accuracy matter.
     * Tends to select medium-sized models like 'base' or 'small'.
     */
    BALANCED("balanced", "Balanced speed and accuracy", 2),
    
    /**
     * Prioritize accuracy over processing speed.
     * Suitable for high-quality transcription needs, legal documents, or critical applications.
     * Tends to select larger, more accurate models like 'medium' or 'large'.
     */
    ACCURACY("accuracy", "High accuracy with slower processing", 3),
    
    /**
     * Maximum accuracy regardless of processing time.
     * Suitable for critical applications where accuracy is paramount.
     * Always selects the largest available model.
     */
    PRECISION("precision", "Maximum accuracy, processing time not a concern", 4);
    
    private final String key;
    private final String description;
    private final int priority;
    
    QualityPreference(String key, String description, int priority) {
        this.key = key;
        this.description = description;
        this.priority = priority;
    }
    
    public String getKey() {
        return key;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getPriority() {
        return priority;
    }
    
    /**
     * Parse quality preference from string.
     * 
     * @param value String representation of quality preference
     * @return QualityPreference enum value, defaults to BALANCED if not found
     */
    public static QualityPreference fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BALANCED;
        }
        
        String normalizedValue = value.toLowerCase().trim();
        for (QualityPreference preference : values()) {
            if (preference.key.equals(normalizedValue) || 
                preference.name().toLowerCase().equals(normalizedValue)) {
                return preference;
            }
        }
        
        return BALANCED; // Default fallback
    }
    
    /**
     * Check if this preference prioritizes speed over accuracy.
     */
    public boolean prioritizesSpeed() {
        return this == SPEED;
    }
    
    /**
     * Check if this preference prioritizes accuracy over speed.
     */
    public boolean prioritizesAccuracy() {
        return this == ACCURACY || this == PRECISION;
    }
    
    /**
     * Check if this preference is balanced between speed and accuracy.
     */
    public boolean isBalanced() {
        return this == BALANCED;
    }
    
    /**
     * Get the maximum acceptable processing time multiplier for this preference.
     * 
     * @return Multiplier for base processing time (1.0 = base, 2.0 = double time acceptable)
     */
    public double getProcessingTimeMultiplier() {
        return switch (this) {
            case SPEED -> 1.0;
            case BALANCED -> 2.0;
            case ACCURACY -> 4.0;
            case PRECISION -> 8.0;
        };
    }
    
    @Override
    public String toString() {
        return String.format("%s (%s)", name(), description);
    }
}

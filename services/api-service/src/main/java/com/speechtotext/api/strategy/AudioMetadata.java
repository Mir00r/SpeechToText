package com.speechtotext.api.strategy;

/**
 * Audio file metadata used for model selection decisions.
 * 
 * Contains essential characteristics about the audio file that influence
 * the optimal model selection, including file size, estimated duration,
 * language, and file format.
 */
public class AudioMetadata {
    
    private final long fileSizeBytes;
    private final double estimatedDurationSeconds;
    private final String language;
    private final String originalFilename;
    private final String mimeType;
    
    /**
     * Constructor for audio metadata.
     * 
     * @param fileSizeBytes Size of the audio file in bytes
     * @param estimatedDurationSeconds Estimated duration in seconds (can be approximate)
     * @param language Language code (ISO 639-1) or "auto" for automatic detection
     */
    public AudioMetadata(long fileSizeBytes, double estimatedDurationSeconds, String language) {
        this(fileSizeBytes, estimatedDurationSeconds, language, null, null);
    }
    
    /**
     * Full constructor for audio metadata.
     * 
     * @param fileSizeBytes Size of the audio file in bytes
     * @param estimatedDurationSeconds Estimated duration in seconds
     * @param language Language code (ISO 639-1) or "auto"
     * @param originalFilename Original filename for format detection
     * @param mimeType MIME type of the audio file
     */
    public AudioMetadata(long fileSizeBytes, double estimatedDurationSeconds, String language, 
                        String originalFilename, String mimeType) {
        this.fileSizeBytes = fileSizeBytes;
        this.estimatedDurationSeconds = estimatedDurationSeconds;
        this.language = language != null ? language : "auto";
        this.originalFilename = originalFilename;
        this.mimeType = mimeType;
    }
    
    // Getters
    public long getFileSizeBytes() { 
        return fileSizeBytes; 
    }
    
    public double getEstimatedDurationSeconds() { 
        return estimatedDurationSeconds; 
    }
    
    public String getLanguage() { 
        return language; 
    }
    
    public String getOriginalFilename() { 
        return originalFilename; 
    }
    
    public String getMimeType() { 
        return mimeType; 
    }
    
    // Utility methods
    
    /**
     * Check if this is a short audio file (< 2 minutes).
     */
    public boolean isShortFile() {
        return estimatedDurationSeconds < 120.0;
    }
    
    /**
     * Check if this is a long audio file (> 30 minutes).
     */
    public boolean isLongFile() {
        return estimatedDurationSeconds > 1800.0;
    }
    
    /**
     * Check if this is a large file by size (> 50MB).
     */
    public boolean isLargeFile() {
        return fileSizeBytes > 50 * 1024 * 1024; // 50MB
    }
    
    /**
     * Check if this is a small file by size (< 5MB).
     */
    public boolean isSmallFile() {
        return fileSizeBytes < 5 * 1024 * 1024; // 5MB
    }
    
    /**
     * Get estimated processing complexity based on duration and size.
     * 
     * @return Complexity score (1.0 = low, 2.0 = medium, 3.0+ = high)
     */
    public double getProcessingComplexity() {
        double durationFactor = Math.min(estimatedDurationSeconds / 1800.0, 2.0); // Cap at 30 min
        double sizeFactor = Math.min(fileSizeBytes / (100.0 * 1024 * 1024), 2.0); // Cap at 100MB
        return 1.0 + Math.max(durationFactor, sizeFactor);
    }
    
    /**
     * Check if language requires special model consideration.
     * Some languages perform better with larger models.
     */
    public boolean requiresLargerModel() {
        if (language == null || "auto".equals(language)) {
            return false;
        }
        
        // Languages that typically benefit from larger models
        String[] complexLanguages = {"zh", "ja", "ko", "ar", "th", "hi", "ru"};
        for (String lang : complexLanguages) {
            if (language.startsWith(lang)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String toString() {
        return String.format("AudioMetadata{size=%d bytes, duration=%.1fs, language='%s', complexity=%.2f}", 
                           fileSizeBytes, estimatedDurationSeconds, language, getProcessingComplexity());
    }
}

package com.speechtotext.api.service;

import com.speechtotext.api.strategy.AudioMetadata;
import com.speechtotext.api.strategy.ModelSelectionStrategy;
import com.speechtotext.api.strategy.QualityPreference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service for intelligent model selection using the Strategy pattern.
 * 
 * This service coordinates different model selection strategies and provides
 * a unified interface for selecting optimal Whisper models based on audio
 * characteristics and quality preferences.
 */
@Service
public class ModelSelectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(ModelSelectionService.class);
    
    private final List<ModelSelectionStrategy> strategies;
    
    @Value("${app.model-selection.strategy:default}")
    private String preferredStrategyName;
    
    @Value("${app.model-selection.fallback-model:base}")
    private String fallbackModel;
    
    @Value("${app.model-selection.enable-auto-selection:true}")
    private boolean enableAutoSelection;
    
    @Autowired
    public ModelSelectionService(List<ModelSelectionStrategy> strategies) {
        this.strategies = strategies;
        logger.info("Initialized ModelSelectionService with {} strategies: {}", 
                   strategies.size(), 
                   strategies.stream().map(ModelSelectionStrategy::getStrategyName).toList());
    }
    
    /**
     * Select optimal model for the given audio file and preferences.
     * 
     * @param file Audio file to analyze
     * @param userSpecifiedModel User-specified model (can be null for auto-selection)
     * @param language Language code or "auto"
     * @param qualityPreference Quality preference for transcription
     * @return Selected model name
     */
    public String selectModel(MultipartFile file, String userSpecifiedModel, 
                             String language, QualityPreference qualityPreference) {
        
        // If auto-selection is disabled, use user-specified model or fallback
        if (!enableAutoSelection) {
            String model = userSpecifiedModel != null ? userSpecifiedModel : fallbackModel;
            logger.debug("Auto-selection disabled, using model: {}", model);
            return model;
        }
        
        // If user specified a model explicitly, respect their choice
        if (userSpecifiedModel != null && !userSpecifiedModel.equals("auto")) {
            logger.debug("User specified model '{}', respecting user choice", userSpecifiedModel);
            return userSpecifiedModel;
        }
        
        // Create audio metadata for analysis
        AudioMetadata metadata = createAudioMetadata(file, language);
        
        // Select strategy and model
        ModelSelectionStrategy strategy = selectStrategy();
        if (strategy == null || !strategy.canHandle(metadata)) {
            logger.warn("No suitable strategy found or metadata cannot be handled, using fallback: {}", fallbackModel);
            return fallbackModel;
        }
        
        try {
            String selectedModel = strategy.selectModel(metadata, qualityPreference);
            logger.info("Strategy '{}' selected model '{}' for file '{}' (size: {} bytes, ~{} seconds)", 
                       strategy.getStrategyName(), selectedModel, file.getOriginalFilename(),
                       file.getSize(), metadata.getEstimatedDurationSeconds());
            
            return validateModel(selectedModel);
            
        } catch (Exception e) {
            logger.error("Error in model selection strategy '{}': {}", strategy.getStrategyName(), e.getMessage(), e);
            return fallbackModel;
        }
    }
    
    /**
     * Select optimal model with simplified parameters.
     * 
     * @param file Audio file to analyze
     * @param language Language code or "auto"
     * @return Selected model name using balanced quality preference
     */
    public String selectModel(MultipartFile file, String language) {
        return selectModel(file, null, language, QualityPreference.BALANCED);
    }
    
    /**
     * Get available model selection strategies.
     * 
     * @return List of strategy names
     */
    public List<String> getAvailableStrategies() {
        return strategies.stream()
                .map(ModelSelectionStrategy::getStrategyName)
                .toList();
    }
    
    /**
     * Create audio metadata from the uploaded file.
     */
    private AudioMetadata createAudioMetadata(MultipartFile file, String language) {
        long fileSize = file.getSize();
        String originalFilename = file.getOriginalFilename();
        String mimeType = file.getContentType();
        
        // Estimate duration based on file size and type
        double estimatedDuration = estimateAudioDuration(fileSize, mimeType);
        
        return new AudioMetadata(fileSize, estimatedDuration, language, originalFilename, mimeType);
    }
    
    /**
     * Estimate audio duration from file size and type.
     * This is a rough approximation - actual duration would require audio analysis.
     */
    private double estimateAudioDuration(long fileSizeBytes, String mimeType) {
        // Rough estimates based on common audio formats and bit rates
        // These are approximations and actual duration may vary significantly
        
        double bytesPerSecond = switch (mimeType != null ? mimeType.toLowerCase() : "") {
            case "audio/wav", "audio/wave", "audio/x-wav" -> 176_400.0; // 16-bit, 44.1kHz stereo
            case "audio/flac", "audio/x-flac" -> 100_000.0; // Compressed lossless
            case "audio/mp3", "audio/mpeg" -> 32_000.0; // 256 kbps
            case "audio/m4a", "audio/mp4", "audio/x-m4a" -> 24_000.0; // 192 kbps AAC
            default -> 50_000.0; // Conservative estimate
        };
        
        double estimatedSeconds = fileSizeBytes / bytesPerSecond;
        
        // Cap the estimate at reasonable bounds
        return Math.max(10.0, Math.min(estimatedSeconds, 7200.0)); // 10 seconds to 2 hours
    }
    
    /**
     * Select the appropriate strategy based on configuration.
     */
    private ModelSelectionStrategy selectStrategy() {
        // Find strategy by name preference
        for (ModelSelectionStrategy strategy : strategies) {
            String strategyName = strategy.getStrategyName().toLowerCase();
            if (strategyName.contains(preferredStrategyName.toLowerCase())) {
                return strategy;
            }
        }
        
        // Fallback to first available strategy
        if (!strategies.isEmpty()) {
            return strategies.get(0);
        }
        
        return null;
    }
    
    /**
     * Validate that the selected model is supported.
     */
    private String validateModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            return fallbackModel;
        }
        
        // Ensure the model is one of the supported Whisper models
        String[] supportedModels = {"tiny", "base", "small", "medium", "large"};
        for (String supportedModel : supportedModels) {
            if (supportedModel.equals(model.toLowerCase().trim())) {
                return supportedModel;
            }
        }
        
        logger.warn("Unsupported model '{}' selected, falling back to '{}'", model, fallbackModel);
        return fallbackModel;
    }
}

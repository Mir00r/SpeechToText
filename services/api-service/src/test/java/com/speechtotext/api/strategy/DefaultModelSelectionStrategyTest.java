package com.speechtotext.api.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class DefaultModelSelectionStrategyTest {
    
    private DefaultModelSelectionStrategy strategy;
    
    @BeforeEach
    void setUp() {
        strategy = new DefaultModelSelectionStrategy();
        // Set default configuration values using reflection
        ReflectionTestUtils.setField(strategy, "enableDynamicSelection", true);
        ReflectionTestUtils.setField(strategy, "fallbackModel", "base");
        ReflectionTestUtils.setField(strategy, "maxProcessingTimeMinutes", 30);
    }
    
    @Test
    void testCanHandle() {
        AudioMetadata validMetadata = new AudioMetadata(1024 * 1024, 120.0, "en");
        assertTrue(strategy.canHandle(validMetadata));
        
        // Test null metadata
        assertFalse(strategy.canHandle(null));
        
        // Test invalid file size
        AudioMetadata invalidSize = new AudioMetadata(0, 120.0, "en");
        assertFalse(strategy.canHandle(invalidSize));
        
        // Test invalid duration
        AudioMetadata invalidDuration = new AudioMetadata(1024, 0, "en");
        assertFalse(strategy.canHandle(invalidDuration));
        
        // Test very long duration (over 2 hours)
        AudioMetadata tooLong = new AudioMetadata(1024, 8000.0, "en");
        assertFalse(strategy.canHandle(tooLong));
    }
    
    @Test
    void testSelectModelForSmallShortFiles() {
        AudioMetadata smallShort = new AudioMetadata(1024 * 1024, 60.0, "en"); // 1MB, 1 minute
        
        assertEquals("tiny", strategy.selectModel(smallShort, QualityPreference.SPEED));
        assertEquals("base", strategy.selectModel(smallShort, QualityPreference.BALANCED));
        assertEquals("small", strategy.selectModel(smallShort, QualityPreference.ACCURACY));
        assertEquals("small", strategy.selectModel(smallShort, QualityPreference.PRECISION));
    }
    
    @Test
    void testSelectModelForLargeFiles() {
        AudioMetadata largeFile = new AudioMetadata(100 * 1024 * 1024, 600.0, "en"); // 100MB, 10 minutes
        
        String speedModel = strategy.selectModel(largeFile, QualityPreference.SPEED);
        String balancedModel = strategy.selectModel(largeFile, QualityPreference.BALANCED);
        String accuracyModel = strategy.selectModel(largeFile, QualityPreference.ACCURACY);
        
        // For large files, models should be capped to prevent memory issues
        assertNotEquals("large", speedModel);
        assertTrue(isValidModel(speedModel));
        assertTrue(isValidModel(balancedModel));
        assertTrue(isValidModel(accuracyModel));
    }
    
    @Test
    void testSelectModelForComplexLanguages() {
        AudioMetadata chineseFile = new AudioMetadata(10 * 1024 * 1024, 300.0, "zh"); // 10MB, 5 minutes
        
        String speedModel = strategy.selectModel(chineseFile, QualityPreference.SPEED);
        String accuracyModel = strategy.selectModel(chineseFile, QualityPreference.ACCURACY);
        
        // Complex languages should get better models
        assertTrue(getModelSize(speedModel) >= getModelSize("base"));
        assertTrue(getModelSize(accuracyModel) >= getModelSize("medium"));
    }
    
    @Test
    void testSelectModelWhenDynamicSelectionDisabled() {
        ReflectionTestUtils.setField(strategy, "enableDynamicSelection", false);
        
        AudioMetadata anyFile = new AudioMetadata(1024 * 1024, 120.0, "en");
        
        assertEquals("base", strategy.selectModel(anyFile, QualityPreference.SPEED));
        assertEquals("base", strategy.selectModel(anyFile, QualityPreference.ACCURACY));
    }
    
    @Test
    void testGetStrategyName() {
        assertEquals("DefaultModelSelection", strategy.getStrategyName());
    }
    
    @Test
    void testFallbackForInvalidMetadata() {
        assertEquals("base", strategy.selectModel(null, QualityPreference.BALANCED));
        
        AudioMetadata invalidMetadata = new AudioMetadata(0, 0, "en");
        assertEquals("base", strategy.selectModel(invalidMetadata, QualityPreference.BALANCED));
    }
    
    private boolean isValidModel(String model) {
        return model != null && 
               (model.equals("tiny") || model.equals("base") || model.equals("small") || 
                model.equals("medium") || model.equals("large"));
    }
    
    private int getModelSize(String model) {
        return switch (model) {
            case "tiny" -> 1;
            case "base" -> 2;
            case "small" -> 3;
            case "medium" -> 4;
            case "large" -> 5;
            default -> 0;
        };
    }
}

package com.speechtotext.api.strategy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AudioMetadataTest {
    
    @Test
    void testBasicProperties() {
        AudioMetadata metadata = new AudioMetadata(1024 * 1024, 120.0, "en");
        
        assertEquals(1024 * 1024, metadata.getFileSizeBytes());
        assertEquals(120.0, metadata.getEstimatedDurationSeconds());
        assertEquals("en", metadata.getLanguage());
    }
    
    @Test
    void testLanguageDefaultsToAuto() {
        AudioMetadata metadata = new AudioMetadata(1024, 60.0, null);
        assertEquals("auto", metadata.getLanguage());
    }
    
    @Test
    void testFileClassifications() {
        // Small file
        AudioMetadata smallFile = new AudioMetadata(2 * 1024 * 1024, 60.0, "en");
        assertTrue(smallFile.isSmallFile());
        assertFalse(smallFile.isLargeFile());
        
        // Large file  
        AudioMetadata largeFile = new AudioMetadata(60 * 1024 * 1024, 60.0, "en");
        assertTrue(largeFile.isLargeFile());
        assertFalse(largeFile.isSmallFile());
        
        // Short file
        AudioMetadata shortFile = new AudioMetadata(1024, 60.0, "en");
        assertTrue(shortFile.isShortFile());
        assertFalse(shortFile.isLongFile());
        
        // Long file
        AudioMetadata longFile = new AudioMetadata(1024, 2000.0, "en");
        assertTrue(longFile.isLongFile());
        assertFalse(longFile.isShortFile());
    }
    
    @Test
    void testProcessingComplexity() {
        // Simple case
        AudioMetadata simple = new AudioMetadata(1024 * 1024, 60.0, "en");
        double complexity = simple.getProcessingComplexity();
        assertTrue(complexity >= 1.0 && complexity <= 3.0);
        
        // More complex case
        AudioMetadata complex = new AudioMetadata(100 * 1024 * 1024, 1800.0, "en");
        double complexComplexity = complex.getProcessingComplexity();
        assertTrue(complexComplexity > complexity);
    }
    
    @Test
    void testRequiresLargerModel() {
        // English - should not require larger model
        AudioMetadata english = new AudioMetadata(1024, 60.0, "en");
        assertFalse(english.requiresLargerModel());
        
        // Chinese - should require larger model
        AudioMetadata chinese = new AudioMetadata(1024, 60.0, "zh");
        assertTrue(chinese.requiresLargerModel());
        
        // Japanese - should require larger model
        AudioMetadata japanese = new AudioMetadata(1024, 60.0, "ja");
        assertTrue(japanese.requiresLargerModel());
        
        // Auto - should not require larger model
        AudioMetadata auto = new AudioMetadata(1024, 60.0, "auto");
        assertFalse(auto.requiresLargerModel());
    }
}

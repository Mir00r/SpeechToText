package com.speechtotext.api.strategy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QualityPreferenceTest {
    
    @Test
    void testFromString() {
        assertEquals(QualityPreference.SPEED, QualityPreference.fromString("speed"));
        assertEquals(QualityPreference.BALANCED, QualityPreference.fromString("BALANCED"));
        assertEquals(QualityPreference.ACCURACY, QualityPreference.fromString("accuracy"));
        assertEquals(QualityPreference.PRECISION, QualityPreference.fromString("precision"));
        
        // Test fallback
        assertEquals(QualityPreference.BALANCED, QualityPreference.fromString("invalid"));
        assertEquals(QualityPreference.BALANCED, QualityPreference.fromString(null));
        assertEquals(QualityPreference.BALANCED, QualityPreference.fromString(""));
    }
    
    @Test
    void testPriorityMethods() {
        assertTrue(QualityPreference.SPEED.prioritizesSpeed());
        assertFalse(QualityPreference.SPEED.prioritizesAccuracy());
        
        assertTrue(QualityPreference.ACCURACY.prioritizesAccuracy());
        assertFalse(QualityPreference.ACCURACY.prioritizesSpeed());
        
        assertTrue(QualityPreference.PRECISION.prioritizesAccuracy());
        assertFalse(QualityPreference.PRECISION.prioritizesSpeed());
        
        assertTrue(QualityPreference.BALANCED.isBalanced());
        assertFalse(QualityPreference.SPEED.isBalanced());
    }
    
    @Test
    void testProcessingTimeMultipliers() {
        assertEquals(1.0, QualityPreference.SPEED.getProcessingTimeMultiplier());
        assertEquals(2.0, QualityPreference.BALANCED.getProcessingTimeMultiplier());
        assertEquals(4.0, QualityPreference.ACCURACY.getProcessingTimeMultiplier());
        assertEquals(8.0, QualityPreference.PRECISION.getProcessingTimeMultiplier());
    }
    
    @Test
    void testPriorityOrdering() {
        assertTrue(QualityPreference.SPEED.getPriority() < QualityPreference.BALANCED.getPriority());
        assertTrue(QualityPreference.BALANCED.getPriority() < QualityPreference.ACCURACY.getPriority());
        assertTrue(QualityPreference.ACCURACY.getPriority() < QualityPreference.PRECISION.getPriority());
    }
}

package com.speechtotext.api.config;

import com.speechtotext.api.strategy.DefaultModelSelectionStrategy;
import com.speechtotext.api.strategy.ModelSelectionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for model selection strategies.
 * 
 * This configuration class manages the setup of model selection strategies
 * and provides default configuration when specific strategies are not available.
 */
@Configuration
@ConfigurationProperties(prefix = "app.model-selection")
public class ModelSelectionConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(ModelSelectionConfig.class);
    
    private String strategy = "default";
    private boolean enableDynamic = true;
    private boolean enableAutoSelection = true;
    private String fallbackModel = "base";
    private int maxProcessingTimeMinutes = 30;
    
    /**
     * Provide a default model selection strategy if no specific strategy is configured.
     */
    @Bean
    @ConditionalOnMissingBean(name = "primaryModelSelectionStrategy")
    public ModelSelectionStrategy primaryModelSelectionStrategy() {
        logger.info("Configuring primary model selection strategy: {}", strategy);
        return new DefaultModelSelectionStrategy();
    }
    
    // Getters and setters for configuration properties
    
    public String getStrategy() {
        return strategy;
    }
    
    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }
    
    public boolean isEnableDynamic() {
        return enableDynamic;
    }
    
    public void setEnableDynamic(boolean enableDynamic) {
        this.enableDynamic = enableDynamic;
    }
    
    public boolean isEnableAutoSelection() {
        return enableAutoSelection;
    }
    
    public void setEnableAutoSelection(boolean enableAutoSelection) {
        this.enableAutoSelection = enableAutoSelection;
    }
    
    public String getFallbackModel() {
        return fallbackModel;
    }
    
    public void setFallbackModel(String fallbackModel) {
        this.fallbackModel = fallbackModel;
    }
    
    public int getMaxProcessingTimeMinutes() {
        return maxProcessingTimeMinutes;
    }
    
    public void setMaxProcessingTimeMinutes(int maxProcessingTimeMinutes) {
        this.maxProcessingTimeMinutes = maxProcessingTimeMinutes;
    }
}

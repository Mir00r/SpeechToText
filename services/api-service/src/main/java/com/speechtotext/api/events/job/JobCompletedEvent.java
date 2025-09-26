package com.speechtotext.api.events.job;

import com.speechtotext.api.events.BaseDomainEvent;
import com.speechtotext.api.events.DomainEvent;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Event fired when a transcription job is completed successfully.
 * 
 * This event captures the final results of the transcription process
 * including the transcript text, timing information, and quality metrics.
 */
public class JobCompletedEvent extends BaseDomainEvent {
    
    private final JobCompletedPayload payload;
    
    private JobCompletedEvent(Builder builder) {
        super(builder);
        this.payload = new JobCompletedPayload(
                builder.transcriptText,
                builder.confidence,
                builder.language,
                builder.modelUsed,
                builder.processingTimeSeconds,
                builder.transcriptUrl,
                builder.timestampsUrl,
                builder.segmentCount,
                builder.wordCount,
                builder.speakerCount,
                builder.diarizationEnabled
        );
    }
    
    @Override
    public Object getPayload() {
        return payload;
    }
    
    /**
     * Payload containing job completion details.
     */
    public static class JobCompletedPayload {
        private final String transcriptText;
        private final BigDecimal confidence;
        private final String language;
        private final String modelUsed;
        private final BigDecimal processingTimeSeconds;
        private final String transcriptUrl;
        private final String timestampsUrl;
        private final Integer segmentCount;
        private final Integer wordCount;
        private final Integer speakerCount;
        private final Boolean diarizationEnabled;
        
        public JobCompletedPayload(String transcriptText, BigDecimal confidence, 
                                  String language, String modelUsed,
                                  BigDecimal processingTimeSeconds, String transcriptUrl,
                                  String timestampsUrl, Integer segmentCount,
                                  Integer wordCount, Integer speakerCount,
                                  Boolean diarizationEnabled) {
            this.transcriptText = transcriptText;
            this.confidence = confidence;
            this.language = language;
            this.modelUsed = modelUsed;
            this.processingTimeSeconds = processingTimeSeconds;
            this.transcriptUrl = transcriptUrl;
            this.timestampsUrl = timestampsUrl;
            this.segmentCount = segmentCount;
            this.wordCount = wordCount;
            this.speakerCount = speakerCount;
            this.diarizationEnabled = diarizationEnabled;
        }
        
        // Getters
        public String getTranscriptText() { return transcriptText; }
        public BigDecimal getConfidence() { return confidence; }
        public String getLanguage() { return language; }
        public String getModelUsed() { return modelUsed; }
        public BigDecimal getProcessingTimeSeconds() { return processingTimeSeconds; }
        public String getTranscriptUrl() { return transcriptUrl; }
        public String getTimestampsUrl() { return timestampsUrl; }
        public Integer getSegmentCount() { return segmentCount; }
        public Integer getWordCount() { return wordCount; }
        public Integer getSpeakerCount() { return speakerCount; }
        public Boolean getDiarizationEnabled() { return diarizationEnabled; }
    }
    
    /**
     * Builder for creating JobCompletedEvent instances.
     */
    public static class Builder extends BaseDomainEvent.Builder {
        private String transcriptText;
        private BigDecimal confidence;
        private String language;
        private String modelUsed;
        private BigDecimal processingTimeSeconds;
        private String transcriptUrl;
        private String timestampsUrl;
        private Integer segmentCount;
        private Integer wordCount;
        private Integer speakerCount;
        private Boolean diarizationEnabled;
        
        public Builder() {
            eventType("JobCompleted");
            aggregateType("TranscriptionJob");
        }
        
        public Builder transcriptText(String transcriptText) {
            this.transcriptText = transcriptText;
            return this;
        }
        
        public Builder confidence(BigDecimal confidence) {
            this.confidence = confidence;
            return this;
        }
        
        public Builder language(String language) {
            this.language = language;
            return this;
        }
        
        public Builder modelUsed(String modelUsed) {
            this.modelUsed = modelUsed;
            return this;
        }
        
        public Builder processingTimeSeconds(BigDecimal processingTimeSeconds) {
            this.processingTimeSeconds = processingTimeSeconds;
            return this;
        }
        
        public Builder transcriptUrl(String transcriptUrl) {
            this.transcriptUrl = transcriptUrl;
            return this;
        }
        
        public Builder timestampsUrl(String timestampsUrl) {
            this.timestampsUrl = timestampsUrl;
            return this;
        }
        
        public Builder segmentCount(Integer segmentCount) {
            this.segmentCount = segmentCount;
            return this;
        }
        
        public Builder wordCount(Integer wordCount) {
            this.wordCount = wordCount;
            return this;
        }
        
        public Builder speakerCount(Integer speakerCount) {
            this.speakerCount = speakerCount;
            return this;
        }
        
        public Builder diarizationEnabled(Boolean diarizationEnabled) {
            this.diarizationEnabled = diarizationEnabled;
            return this;
        }
        
        @Override
        public Builder aggregateId(String aggregateId) {
            super.aggregateId(aggregateId);
            return this;
        }
        
        @Override
        public Builder correlationId(String correlationId) {
            super.correlationId(correlationId);
            return this;
        }
        
        @Override
        public Builder initiatedBy(String initiatedBy) {
            super.initiatedBy(initiatedBy);
            return this;
        }
        
        @Override
        public Builder metadata(String key, Object value) {
            super.metadata(key, value);
            return this;
        }
        
        @Override
        public Builder metadata(Map<String, Object> metadata) {
            super.metadata(metadata);
            return this;
        }
        
        @Override
        public Builder sequenceNumber(Long sequenceNumber) {
            super.sequenceNumber(sequenceNumber);
            return this;
        }
        
        @Override
        public DomainEvent build() {
            if (transcriptText == null || language == null) {
                throw new IllegalArgumentException("transcriptText and language are required");
            }
            return new JobCompletedEvent(this);
        }
    }
}

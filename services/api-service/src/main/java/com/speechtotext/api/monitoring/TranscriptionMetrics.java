package com.speechtotext.api.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Custom metrics for monitoring transcription service performance.
 */
@Component
public class TranscriptionMetrics {
    
    private final Counter uploadCounter;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Timer transcriptionTimer;
    private final Counter syncProcessingCounter;
    private final Counter asyncProcessingCounter;
    
    @Autowired
    public TranscriptionMetrics(MeterRegistry meterRegistry) {
        this.uploadCounter = Counter.builder("transcription.uploads.total")
                .description("Total number of file uploads")
                .register(meterRegistry);
                
        this.successCounter = Counter.builder("transcription.success.total")
                .description("Total number of successful transcriptions")
                .register(meterRegistry);
                
        this.failureCounter = Counter.builder("transcription.failures.total")
                .description("Total number of failed transcriptions")
                .register(meterRegistry);
                
        this.transcriptionTimer = Timer.builder("transcription.duration")
                .description("Time taken for transcription processing")
                .register(meterRegistry);
                
        this.syncProcessingCounter = Counter.builder("transcription.sync.total")
                .description("Total number of synchronous processing requests")
                .register(meterRegistry);
                
        this.asyncProcessingCounter = Counter.builder("transcription.async.total")
                .description("Total number of asynchronous processing requests")
                .register(meterRegistry);
    }
    
    public void recordUpload() {
        uploadCounter.increment();
    }
    
    public void recordSuccess() {
        successCounter.increment();
    }
    
    public void recordFailure() {
        failureCounter.increment();
    }
    
    public Timer.Sample startTranscriptionTimer() {
        return Timer.start();
    }
    
    public void recordTranscriptionTime(Timer.Sample sample) {
        sample.stop(transcriptionTimer);
    }
    
    public void recordSyncProcessing() {
        syncProcessingCounter.increment();
    }
    
    public void recordAsyncProcessing() {
        asyncProcessingCounter.increment();
    }
}

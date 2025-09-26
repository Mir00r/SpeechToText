package com.speechtotext.api.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speechtotext.api.exception.ExternalServiceException;
import com.speechtotext.api.dto.TranscriptionCallbackRequest;
import com.speechtotext.api.trace.TraceContext;
import com.speechtotext.api.trace.TracingHelper;

// Resilience4j imports - commented out temporarily for compilation
// import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
// import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
// import io.github.resilience4j.retry.annotation.Retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class TranscriptionServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(TranscriptionServiceClient.class);

    private final RestTemplate restTemplate;
    private final String callbackBaseUrl;
    private final ObjectMapper objectMapper;
    private final TracingHelper tracingHelper;

    public TranscriptionServiceClient(
            @Qualifier("transcriptionRestTemplate") RestTemplate restTemplate,
            @Value("${app.callback.base-url:http://api-service:8080}") String callbackBaseUrl,
            TracingHelper tracingHelper) {
        this.restTemplate = restTemplate;
        this.callbackBaseUrl = callbackBaseUrl;
        this.objectMapper = new ObjectMapper();
        this.tracingHelper = tracingHelper;
    }

    /**
     * Submit transcription job with circuit breaker protection.
     * Falls back to graceful error handling if service is unavailable.
     */
    // Circuit breaker annotations commented out temporarily
    // @CircuitBreaker(name = "transcriptionService", fallbackMethod = "fallbackSubmitTranscription")
    // @Retry(name = "transcriptionService")
    public void submitTranscriptionJob(UUID jobId, String s3Url, boolean enableDiarization, boolean enableAlignment) {
        try {
            String callbackUrl = callbackBaseUrl + "/internal/v1/transcriptions/" + jobId + "/callback";
            
            TranscriptionRequest request = new TranscriptionRequest(
                    jobId.toString(),
                    s3Url,
                    callbackUrl,
                    enableDiarization,
                    enableAlignment
            );

            // Create headers with tracing information
            HttpHeaders headers = tracingHelper.createTracingHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<TranscriptionRequest> entity = new HttpEntity<>(request, headers);

            logger.info("Submitting transcription job {} to transcription service [correlationId={}]", 
                       jobId, TraceContext.getCorrelationId());
            
            ResponseEntity<String> response = restTemplate.exchange(
                    "/transcribe",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully submitted transcription job {} [correlationId={}]", 
                           jobId, TraceContext.getCorrelationId());
            } else {
                logger.error("Failed to submit transcription job {}, status: {} [correlationId={}]", 
                            jobId, response.getStatusCode(), TraceContext.getCorrelationId());
                throw new ExternalServiceException.TranscriptionServiceException("Failed to submit transcription job: " + response.getStatusCode());
            }

        } catch (RestClientException e) {
            logger.error("Error calling transcription service for job {} [correlationId={}]", 
                        jobId, TraceContext.getCorrelationId(), e);
            throw new ExternalServiceException.TranscriptionServiceException("Failed to communicate with transcription service", e);
        }
    }

    /**
     * Submit transcription job for synchronous processing with circuit breaker and time limiter.
     */
    // Circuit breaker annotations commented out temporarily  
    // @CircuitBreaker(name = "transcriptionService", fallbackMethod = "fallbackSubmitTranscriptionSync")
    // @TimeLimiter(name = "transcriptionService")
    // @Retry(name = "transcriptionService")
    public CompletableFuture<TranscriptionCallbackRequest> submitTranscriptionJobSyncAsync(UUID jobId, String s3Url, 
                                                                                          boolean enableDiarization, boolean enableAlignment, 
                                                                                          int timeoutSeconds) {
        return CompletableFuture.supplyAsync(() -> submitTranscriptionJobSyncInternal(jobId, s3Url, enableDiarization, enableAlignment, timeoutSeconds));
    }
    
    /**
     * Original synchronous method - kept for backward compatibility.
     */
    public TranscriptionCallbackRequest submitTranscriptionJobSync(UUID jobId, String s3Url, 
                                                                  boolean enableDiarization, boolean enableAlignment, 
                                                                  int timeoutSeconds) {
        return submitTranscriptionJobSyncInternal(jobId, s3Url, enableDiarization, enableAlignment, timeoutSeconds);
    }
    
    /**
     * Internal method for synchronous transcription processing.
     */
    private TranscriptionCallbackRequest submitTranscriptionJobSyncInternal(UUID jobId, String s3Url, 
                                                                           boolean enableDiarization, boolean enableAlignment, 
                                                                           int timeoutSeconds) {
        logger.info("Submitting sync transcription job {} to service with timeout {} seconds", jobId, timeoutSeconds);
        
        try {
            String callbackUrl = callbackBaseUrl + "/internal/v1/transcriptions/" + jobId + "/callback";
            
            SyncTranscriptionRequest request = new SyncTranscriptionRequest(
                    jobId.toString(),
                    s3Url,
                    callbackUrl,
                    enableDiarization,
                    enableAlignment,
                    true // synchronous
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<SyncTranscriptionRequest> entity = new HttpEntity<>(request, headers);

            logger.info("Submitting synchronous transcription job {} to transcription service", jobId);
            ResponseEntity<String> response = restTemplate.exchange(
                    "/transcribe",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.error("Sync transcription failed for job {}, status: {}", jobId, response.getStatusCode());
                throw new ExternalServiceException.TranscriptionServiceException("Sync transcription failed: " + response.getStatusCode());
            }

            // Parse the response as a transcription result
            try {
                logger.info("Successfully received sync transcription result for job {}", jobId);
                return objectMapper.readValue(response.getBody(), TranscriptionCallbackRequest.class);
            } catch (Exception e) {
                logger.error("Failed to parse synchronous transcription response for job {}", jobId, e);
                throw new ExternalServiceException.InvalidServiceResponseException("transcription-service", "Failed to parse response: " + e.getMessage());
            }

        } catch (RestClientException e) {
            logger.error("Error calling transcription service for sync job {}", jobId, e);
            throw new ExternalServiceException.TranscriptionServiceException("Failed to communicate with transcription service", e);
        }
    }
    
    // ================================
    // Circuit Breaker Fallback Methods
    // ================================
    
    /**
     * Fallback method for async transcription submission when circuit breaker is open.
     */
    public void fallbackSubmitTranscription(UUID jobId, String s3Url, boolean enableDiarization, 
                                           boolean enableAlignment, Exception ex) {
        logger.error("Circuit breaker is open for transcription service. Job {} will be marked as failed.", jobId, ex);
        throw new ExternalServiceException.ServiceUnavailableException("transcription-service");
    }
    
    /**
     * Fallback method for sync transcription when circuit breaker is open or timeout occurs.
     */
    public CompletableFuture<TranscriptionCallbackRequest> fallbackSubmitTranscriptionSync(UUID jobId, String s3Url, 
                                                                                           boolean enableDiarization, 
                                                                                           boolean enableAlignment, 
                                                                                           int timeoutSeconds, 
                                                                                           Exception ex) {
        logger.error("Circuit breaker is open or timeout occurred for sync transcription service. Job {} will be processed asynchronously.", 
                    jobId, ex);
        
        return CompletableFuture.supplyAsync(() -> {
            // Attempt to submit as async job instead
            try {
                submitTranscriptionJob(jobId, s3Url, enableDiarization, enableAlignment);
                
                // Return a placeholder response indicating async processing
                return new TranscriptionCallbackRequest(
                    "PROCESSING",
                    null, // transcriptText
                    null, // s3TranscriptUrl
                    "Switched to asynchronous processing due to service issues", // errorMessage
                    null, // processingDurationMs
                    null, // audioDurationS
                    null, // segments
                    null, // words
                    null  // metadata
                );
                
            } catch (Exception asyncEx) {
                logger.error("Failed to submit job {} even as async after circuit breaker fallback", jobId, asyncEx);
                throw new ExternalServiceException.ServiceUnavailableException("transcription-service");
            }
        });
    }
    
    /**
     * Health check method for transcription service (used by health indicators).
     */
    // Circuit breaker annotation commented out temporarily
    // @CircuitBreaker(name = "transcriptionService", fallbackMethod = "fallbackHealthCheck")
    public boolean isTranscriptionServiceHealthy() {
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                "/health",
                HttpMethod.GET,
                null,
                String.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.warn("Health check failed for transcription service", e);
            return false;
        }
    }
    
    /**
     * Fallback for health check when circuit breaker is open.
     */
    public boolean fallbackHealthCheck(Exception ex) {
        logger.warn("Transcription service health check failed - circuit breaker is open", ex);
        return false;
    }

    // ================================
    // Request DTOs
    // ================================

    public static class TranscriptionRequest {
        @JsonProperty("job_id")
        public final String jobId;
        
        @JsonProperty("s3_url")
        public final String s3Url;
        
        @JsonProperty("callback_url")
        public final String callbackUrl;
        
        @JsonProperty("enable_diarization")
        public final boolean enableDiarization;
        
        @JsonProperty("enable_alignment")
        public final boolean enableAlignment;

        public TranscriptionRequest(String jobId, String s3Url, String callbackUrl, 
                                  boolean enableDiarization, boolean enableAlignment) {
            this.jobId = jobId;
            this.s3Url = s3Url;
            this.callbackUrl = callbackUrl;
            this.enableDiarization = enableDiarization;
            this.enableAlignment = enableAlignment;
        }
    }

    public static class SyncTranscriptionRequest extends TranscriptionRequest {
        @JsonProperty("synchronous")
        public final boolean synchronous;

        public SyncTranscriptionRequest(String jobId, String s3Url, String callbackUrl, 
                                      boolean enableDiarization, boolean enableAlignment, boolean synchronous) {
            super(jobId, s3Url, callbackUrl, enableDiarization, enableAlignment);
            this.synchronous = synchronous;
        }
    }
}

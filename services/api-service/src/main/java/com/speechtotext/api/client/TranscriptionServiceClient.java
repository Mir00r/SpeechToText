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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Component
public class TranscriptionServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(TranscriptionServiceClient.class);

    private final RestTemplate restTemplate;
    private final String callbackBaseUrl;

    public TranscriptionServiceClient(
            @Qualifier("transcriptionRestTemplate") RestTemplate restTemplate,
            @Value("${app.callback.base-url:http://api-service:8080}") String callbackBaseUrl) {
        this.restTemplate = restTemplate;
        this.callbackBaseUrl = callbackBaseUrl;
    }

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

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<TranscriptionRequest> entity = new HttpEntity<>(request, headers);

            logger.info("Submitting transcription job {} to transcription service", jobId);
            ResponseEntity<String> response = restTemplate.exchange(
                    "/transcribe",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully submitted transcription job {}", jobId);
            } else {
                logger.error("Failed to submit transcription job {}, status: {}", jobId, response.getStatusCode());
                throw new TranscriptionServiceException("Failed to submit transcription job: " + response.getStatusCode());
            }

        } catch (RestClientException e) {
            logger.error("Error calling transcription service for job {}", jobId, e);
            throw new TranscriptionServiceException("Failed to communicate with transcription service", e);
        }
    }

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

    public static class TranscriptionServiceException extends RuntimeException {
        public TranscriptionServiceException(String message) {
            super(message);
        }

        public TranscriptionServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

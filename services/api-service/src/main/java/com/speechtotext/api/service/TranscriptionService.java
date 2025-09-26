package com.speechtotext.api.service;

import com.speechtotext.api.client.TranscriptionServiceClient;
import com.speechtotext.api.dto.TranscriptionCallbackRequest;
import com.speechtotext.api.dto.TranscriptionResponse;
import com.speechtotext.api.dto.TranscriptionUploadRequest;
import com.speechtotext.api.events.DomainEvent;
import com.speechtotext.api.events.factory.EventFactory;
import com.speechtotext.api.events.publisher.EventPublisher;
import com.speechtotext.api.exception.ExternalServiceException;
import com.speechtotext.api.infra.s3.S3ClientAdapter;
import com.speechtotext.api.mapper.JobMapper;
import com.speechtotext.api.model.JobEntity;
import com.speechtotext.api.repository.JobRepository;
import com.speechtotext.api.strategy.QualityPreference;
import com.speechtotext.api.trace.TraceConstants;
import com.speechtotext.api.trace.TraceContext;
import com.speechtotext.api.trace.TracingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling transcription operations.
 */
@Service
@Transactional
public class TranscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(TranscriptionService.class);

    // Allowed MIME types for audio files
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
        "audio/wav", "audio/wave", "audio/x-wav",
        "audio/mpeg", "audio/mp3",
        "audio/mp4", "audio/m4a", "audio/x-m4a",
        "audio/flac", "audio/x-flac"
    );

    private final JobRepository jobRepository;
    private final S3ClientAdapter s3ClientAdapter;
    private final JobMapper jobMapper;
    private final TranscriptionServiceClient transcriptionServiceClient;
    private final ModelSelectionService modelSelectionService;
    private final TracingHelper tracingHelper;
    private final EventPublisher eventPublisher;
    private final EventFactory eventFactory;

    @Value("${app.upload.max-file-size:104857600}") // 100MB default
    private long maxFileSize;

    @Value("${app.transcription.sync-threshold-seconds:60}")
    private int syncThresholdSeconds;

    @Value("${app.transcription.sync-timeout-seconds:120}")
    private int syncTimeoutSeconds;

    public TranscriptionService(JobRepository jobRepository, 
                              S3ClientAdapter s3ClientAdapter,
                              JobMapper jobMapper,
                              TranscriptionServiceClient transcriptionServiceClient,
                              ModelSelectionService modelSelectionService,
                              TracingHelper tracingHelper,
                              EventPublisher eventPublisher,
                              EventFactory eventFactory) {
        this.jobRepository = jobRepository;
        this.s3ClientAdapter = s3ClientAdapter;
        this.jobMapper = jobMapper;
        this.transcriptionServiceClient = transcriptionServiceClient;
        this.modelSelectionService = modelSelectionService;
        this.tracingHelper = tracingHelper;
        this.eventPublisher = eventPublisher;
        this.eventFactory = eventFactory;
    }

    /**
     * Create a new transcription job.
     */
    public Object createTranscriptionJob(MultipartFile file, TranscriptionUploadRequest request) {
        logger.info("Creating transcription job for file: {} [correlationId={}]", 
                   file.getOriginalFilename(), TraceContext.getCorrelationId());

        // Validate file
        validateFile(file);

        try {
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String filename = generateUniqueFilename(originalFilename);

            // Upload file to S3 with tracing
            String storageUrl = tracingHelper.executeS3Operation(filename, TraceConstants.OP_S3_UPLOAD, 
                () -> {
                    TraceContext.setFileContext(filename, file.getSize());
                    return s3ClientAdapter.uploadFile(file, filename);
                });

            // Select optimal model using strategy pattern
            QualityPreference qualityPreference = QualityPreference.fromString(request.quality());
            String selectedModel = tracingHelper.executeWithTraceSupplier(TraceConstants.OP_MODEL_SELECTION,
                () -> modelSelectionService.selectModel(file, request.model(), request.language(), qualityPreference));

            // Create job entity
            JobEntity job = new JobEntity(filename, originalFilename, storageUrl);
            job.setModel(selectedModel);
            job.setLanguage(request.language());
            job.setFileSizeBytes(file.getSize());

            logger.info("Selected model '{}' for file '{}' (quality: {}, user model: {}) [correlationId={}]", 
                       selectedModel, originalFilename, qualityPreference, request.model(), TraceContext.getCorrelationId());

            // Save job to database with tracing
            final JobEntity jobToSave = job;
            job = tracingHelper.executeDatabaseOperation("save_job", () -> jobRepository.save(jobToSave));
            
            // Set job context for subsequent operations
            TraceContext.setJobContext(job.getId().toString());

            // Publish job created event
            eventPublisher.publish(eventFactory.createJobCreatedEvent(
                    job.getId().toString(),
                    job.getFilename(),
                    job.getOriginalFilename(),
                    job.getStorageUrl(),
                    selectedModel,
                    request.language(),
                    request.quality(),
                    file.getSize(),
                    null, // estimated duration - could be calculated
                    request.diarize(),
                    request.synchronous(),
                    getClientIpFromTrace(),
                    null // user agent - could be extracted from request
            ));

            logger.info("Created transcription job {} for file {}", job.getId(), originalFilename);

            // Determine processing mode (sync vs async)
            boolean shouldProcessSync = shouldProcessSynchronously(file, request);
            
            if (shouldProcessSync) {
                return processSynchronously(job, request);
            } else {
                return processAsynchronously(job, request);
            }

        } catch (Exception e) {
            logger.error("Failed to create transcription job for file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to create transcription job", e);
        }
    }

    /**
     * Get transcription job by ID.
     */
    @Transactional(readOnly = true)
    public TranscriptionResponse getTranscriptionJob(UUID jobId) {
        logger.debug("Retrieving transcription job: {}", jobId);

        JobEntity job = jobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        return jobMapper.toTranscriptionResponse(job);
    }

    /**
     * Download transcript as plain text.
     */
    @Transactional(readOnly = true)
    public Resource downloadTranscript(UUID jobId) {
        logger.debug("Downloading transcript for job: {}", jobId);

        JobEntity job = jobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        if (job.getStatus() != JobEntity.JobStatus.COMPLETED) {
            throw new IllegalStateException("Transcription is not completed yet");
        }

        if (job.getTranscriptText() == null || job.getTranscriptText().isEmpty()) {
            throw new IllegalStateException("No transcript available");
        }

        // Return transcript as downloadable resource
        InputStream inputStream = java.io.ByteArrayInputStream.class
            .cast(new java.io.ByteArrayInputStream(job.getTranscriptText().getBytes(StandardCharsets.UTF_8)));
        
        return new InputStreamResource(inputStream);
    }

    /**
     * Handle transcription callback from the transcription service.
     */
    public void handleTranscriptionCallback(UUID jobId, TranscriptionCallbackRequest callbackRequest) {
        logger.info("Processing transcription callback for job {}", jobId);

        JobEntity job = jobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        try {
            // Update job based on callback status
            switch (callbackRequest.status().toLowerCase()) {
                case "completed" -> {
                    job.setStatus(JobEntity.JobStatus.COMPLETED);
                    job.setTranscriptText(callbackRequest.transcriptText());
                    
                    // Store detailed results as JSON if available
                    if (callbackRequest.segments() != null || callbackRequest.speakerSegments() != null) {
                        job.setTimestampsJson(buildTimestampsJson(callbackRequest));
                    }
                    
                    // Processing duration will be calculated from startedAt/finishedAt
                    
                    job.setFinishedAt(LocalDateTime.now());
                    
                    // Publish job status changed event first
                    try {
                        Long processingTime = job.getStartedAt() != null 
                            ? java.time.Duration.between(job.getStartedAt(), job.getFinishedAt()).toMillis()
                            : null;
                            
                        DomainEvent statusChangedEvent = eventFactory.createJobStatusChangedEvent(
                            jobId.toString(),
                            JobEntity.JobStatus.PROCESSING, // previous status (assuming it was processing)
                            JobEntity.JobStatus.COMPLETED, // new status
                            "Transcription completed successfully", // reason for change
                            null, // no error message
                            processingTime // processing time in milliseconds
                        );
                        eventPublisher.publish(statusChangedEvent);
                    } catch (Exception eventException) {
                        logger.warn("Failed to publish job status changed event for job {}", jobId, eventException);
                    }
                    
                    // Publish job completed event
                    try {
                        // Calculate processing time
                        BigDecimal processingTime = job.getStartedAt() != null && job.getFinishedAt() != null 
                            ? BigDecimal.valueOf(java.time.Duration.between(job.getStartedAt(), job.getFinishedAt()).toSeconds())
                            : BigDecimal.ZERO;
                            
                        DomainEvent completedEvent = eventFactory.createJobCompletedEvent(
                            jobId.toString(),
                            job.getTranscriptText(),
                            BigDecimal.valueOf(0.95), // Default confidence - could be extracted from callback if available
                            "en", // Default language - could be extracted from job parameters
                            "whisper", // Default model - could be extracted from job parameters
                            processingTime,
                            null, // transcript URL - not applicable in this context
                            null, // timestamps URL - not applicable in this context
                            null, // segment count - could be calculated if available
                            null, // word count - could be calculated if available
                            null, // speaker count - could be extracted from callback if available
                            false // diarization enabled - could be extracted from job parameters
                        );
                        eventPublisher.publish(completedEvent);
                    } catch (Exception eventException) {
                        logger.warn("Failed to publish job completed event for job {}", jobId, eventException);
                    }
                    
                    logger.info("Job {} completed successfully", jobId);
                }
                case "failed" -> {
                    job.setStatus(JobEntity.JobStatus.FAILED);
                    job.setErrorMessage(callbackRequest.errorMessage());
                    job.setFinishedAt(LocalDateTime.now());
                    
                    // Publish job status changed event for failed state
                    try {
                        Long processingTime = job.getStartedAt() != null 
                            ? java.time.Duration.between(job.getStartedAt(), LocalDateTime.now()).toMillis()
                            : null;
                            
                        DomainEvent statusChangedEvent = eventFactory.createJobStatusChangedEvent(
                            jobId.toString(),
                            JobEntity.JobStatus.PROCESSING, // previous status (assuming it was processing)
                            JobEntity.JobStatus.FAILED, // new status
                            "Transcription failed", // reason for change
                            callbackRequest.errorMessage(), // error message
                            processingTime // processing time in milliseconds
                        );
                        eventPublisher.publish(statusChangedEvent);
                    } catch (Exception eventException) {
                        logger.warn("Failed to publish job status changed event for job {}", jobId, eventException);
                    }
                    
                    logger.warn("Job {} failed: {}", jobId, callbackRequest.errorMessage());
                }
                case "processing" -> {
                    JobEntity.JobStatus previousStatus = job.getStatus();
                    job.setStatus(JobEntity.JobStatus.PROCESSING);
                    if (job.getStartedAt() == null) {
                        job.setStartedAt(LocalDateTime.now());
                    }
                    
                    // Publish job status changed event for processing state
                    try {
                        DomainEvent statusChangedEvent = eventFactory.createJobStatusChangedEvent(
                            jobId.toString(),
                            previousStatus, // previous status
                            JobEntity.JobStatus.PROCESSING, // new status
                            "Transcription started", // reason for change
                            null, // no error message
                            null // no processing time yet
                        );
                        eventPublisher.publish(statusChangedEvent);
                    } catch (Exception eventException) {
                        logger.warn("Failed to publish job status changed event for job {}", jobId, eventException);
                    }
                    
                    logger.info("Job {} is now processing", jobId);
                }
                default -> {
                    logger.warn("Unknown callback status '{}' for job {}", callbackRequest.status(), jobId);
                    return;
                }
            }

            jobRepository.save(job);
            
        } catch (Exception e) {
            logger.error("Error processing callback for job {}", jobId, e);
            // Update job to failed state
            job.setStatus(JobEntity.JobStatus.FAILED);
            job.setErrorMessage("Internal error processing transcription result");
            job.setFinishedAt(LocalDateTime.now());
            jobRepository.save(job);
        }
    }

    /**
     * Build timestamps JSON from callback data.
     */
    private String buildTimestampsJson(TranscriptionCallbackRequest callbackRequest) {
        try {
            // Simple JSON construction - in a real app, you might use ObjectMapper
            StringBuilder json = new StringBuilder("{");
            
            if (callbackRequest.segments() != null) {
                json.append("\"segments\":[");
                boolean first = true;
                for (var segment : callbackRequest.segments()) {
                    if (!first) json.append(",");
                    json.append(String.format(
                        "{\"start\":%.3f,\"end\":%.3f,\"text\":\"%s\"}",
                        segment.start(), segment.end(), 
                        segment.text().replace("\"", "\\\"")
                    ));
                    first = false;
                }
                json.append("]");
            }
            
            if (callbackRequest.speakerSegments() != null && !callbackRequest.speakerSegments().isEmpty()) {
                if (callbackRequest.segments() != null) json.append(",");
                json.append("\"speakers\":[");
                boolean first = true;
                for (var speaker : callbackRequest.speakerSegments()) {
                    if (!first) json.append(",");
                    json.append(String.format(
                        "{\"speaker\":\"%s\",\"start\":%.3f,\"end\":%.3f}",
                        speaker.speaker(), speaker.start(), speaker.end()
                    ));
                    first = false;
                }
                json.append("]");
            }
            
            json.append("}");
            return json.toString();
        } catch (Exception e) {
            logger.warn("Failed to build timestamps JSON", e);
            return "{}";
        }
    }

    /**
     * Validate uploaded file.
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                String.format("File size %d bytes exceeds maximum allowed size %d bytes", 
                    file.getSize(), maxFileSize));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                String.format("File type '%s' not supported. Allowed types: %s", 
                    contentType, String.join(", ", ALLOWED_MIME_TYPES)));
        }
    }

    /**
     * Generate unique filename.
     */
    private String generateUniqueFilename(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * Determine if file should be processed synchronously.
     */
    private boolean shouldProcessSynchronously(MultipartFile file, TranscriptionUploadRequest request) {
        // If user explicitly requested sync/async, honor that
        if (request.synchronous() != null) {
            return request.synchronous();
        }
        
        // For very small files, default to sync
        if (file.getSize() < 1_000_000) { // < 1MB
            return true;
        }
        
        // For larger files, estimate duration and decide
        // Rough estimation: 1 minute of audio ≈ 1MB (varies by quality)
        double estimatedDurationSeconds = file.getSize() / 1_000_000.0 * 60;
        
        logger.debug("File size: {} bytes, estimated duration: {} seconds, threshold: {} seconds", 
                    file.getSize(), estimatedDurationSeconds, syncThresholdSeconds);
        
        return estimatedDurationSeconds <= syncThresholdSeconds;
    }

    /**
     * Process transcription synchronously and return complete result.
     */
    private Object processSynchronously(JobEntity job, TranscriptionUploadRequest request) {
        logger.info("Processing job {} synchronously", job.getId());
        
        try {
            // Update status to processing
            job.setStatus(JobEntity.JobStatus.PROCESSING);
            job.setStartedAt(LocalDateTime.now());
            jobRepository.save(job);

            // Call transcription service synchronously
            TranscriptionCallbackRequest result = transcriptionServiceClient.submitTranscriptionJobSync(
                job.getId(),
                job.getStorageUrl(),
                request.diarize() != null ? request.diarize() : false,
                true, // always enable alignment for better results
                syncTimeoutSeconds
            );

            // Process the result immediately
            handleTranscriptionCallback(job.getId(), result);

            // Return the completed transcription response
            return jobMapper.toTranscriptionResponse(jobRepository.findById(job.getId()).orElse(job));

        } catch (ExternalServiceException.TranscriptionServiceException e) {
            logger.error("Synchronous transcription failed for job {}", job.getId(), e);
            
            // Update job status to failed
            job.setStatus(JobEntity.JobStatus.FAILED);
            job.setErrorMessage("Synchronous transcription failed: " + e.getMessage());
            job.setFinishedAt(LocalDateTime.now());
            jobRepository.save(job);
            
            // Return error response
            throw new RuntimeException("Synchronous transcription failed: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during synchronous processing for job {}", job.getId(), e);
            
            // Update job status to failed
            job.setStatus(JobEntity.JobStatus.FAILED);
            job.setErrorMessage("Internal error during synchronous processing");
            job.setFinishedAt(LocalDateTime.now());
            jobRepository.save(job);
            
            throw new RuntimeException("Synchronous transcription failed due to internal error");
        }
    }

    /**
     * Process transcription asynchronously and return job info.
     */
    private Object processAsynchronously(JobEntity job, TranscriptionUploadRequest request) {
        logger.info("Processing job {} asynchronously", job.getId());
        
        try {
            // Submit job to transcription service for async processing
            transcriptionServiceClient.submitTranscriptionJob(
                job.getId(),
                job.getStorageUrl(),
                request.diarize() != null ? request.diarize() : false,
                true // always enable alignment for better results
            );
            
            // Update status to processing
            job.setStatus(JobEntity.JobStatus.PROCESSING);
            job.setStartedAt(LocalDateTime.now());
            jobRepository.save(job);
            
            // Return async job response
            return jobMapper.toTranscriptionJobResponse(job);
            
        } catch (ExternalServiceException.TranscriptionServiceException e) {
            logger.error("Failed to submit job {} to transcription service", job.getId(), e);
            
            // Update job status to failed
            job.setStatus(JobEntity.JobStatus.FAILED);
            job.setErrorMessage("Failed to submit job to transcription service: " + e.getMessage());
            job.setFinishedAt(LocalDateTime.now());
            jobRepository.save(job);
            
            // Return the failed job response
            return jobMapper.toTranscriptionJobResponse(job);
        }
    }

    /**
     * Get client IP from trace context or default value.
     */
    private String getClientIpFromTrace() {
        // Since we don't have direct access to client IP in TraceContext yet, 
        // we'll use a default. This could be enhanced to extract from request context.
        return "unknown";
    }
}

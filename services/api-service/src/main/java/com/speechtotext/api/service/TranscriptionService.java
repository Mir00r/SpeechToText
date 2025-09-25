package com.speechtotext.api.service;

import com.speechtotext.api.dto.TranscriptionJobResponse;
import com.speechtotext.api.dto.TranscriptionResponse;
import com.speechtotext.api.dto.TranscriptionUploadRequest;
import com.speechtotext.api.infra.s3.S3ClientAdapter;
import com.speechtotext.api.mapper.JobMapper;
import com.speechtotext.api.model.JobEntity;
import com.speechtotext.api.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
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

    @Value("${app.upload.max-file-size:104857600}") // 100MB default
    private long maxFileSize;

    @Value("${app.transcription.sync-threshold-seconds:60}")
    private int syncThresholdSeconds;

    public TranscriptionService(JobRepository jobRepository, 
                              S3ClientAdapter s3ClientAdapter,
                              JobMapper jobMapper) {
        this.jobRepository = jobRepository;
        this.s3ClientAdapter = s3ClientAdapter;
        this.jobMapper = jobMapper;
    }

    /**
     * Create a new transcription job.
     */
    public Object createTranscriptionJob(MultipartFile file, TranscriptionUploadRequest request) {
        logger.info("Creating transcription job for file: {}", file.getOriginalFilename());

        // Validate file
        validateFile(file);

        try {
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String filename = generateUniqueFilename(originalFilename);

            // Upload file to S3
            String storageUrl = s3ClientAdapter.uploadFile(file, filename);

            // Create job entity
            JobEntity job = new JobEntity(filename, originalFilename, storageUrl);
            job.setModel(request.model());
            job.setLanguage(request.language());
            job.setFileSizeBytes(file.getSize());

            // Save job to database
            job = jobRepository.save(job);

            logger.info("Created transcription job {} for file {}", job.getId(), originalFilename);

            // For now, always return async response (sync processing will be added in M5)
            return jobMapper.toTranscriptionJobResponse(job);

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
     * Update job status and transcript (called by transcription service callback).
     */
    public void updateJobResult(UUID jobId, JobEntity.JobStatus status, String transcriptText, 
                               String timestampsJson, String errorMessage) {
        logger.info("Updating job {} with status: {}", jobId, status);

        JobEntity job = jobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        job.setStatus(status);
        job.setTranscriptText(transcriptText);
        job.setTimestampsJson(timestampsJson);
        job.setErrorMessage(errorMessage);

        if (status == JobEntity.JobStatus.PROCESSING && job.getStartedAt() == null) {
            job.setStartedAt(LocalDateTime.now());
        }

        if (status == JobEntity.JobStatus.COMPLETED || status == JobEntity.JobStatus.FAILED) {
            job.setFinishedAt(LocalDateTime.now());
        }

        jobRepository.save(job);
        logger.info("Updated job {} with status: {}", jobId, status);
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
}

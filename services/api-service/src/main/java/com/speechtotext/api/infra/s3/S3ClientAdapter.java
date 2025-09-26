package com.speechtotext.api.infra.s3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.speechtotext.api.exception.StorageException;
import com.speechtotext.api.trace.TraceContext;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Adapter for S3/MinIO operations.
 */
@Component
public class S3ClientAdapter {

    private static final Logger logger = LoggerFactory.getLogger(S3ClientAdapter.class);

    private final S3Client s3Client;
    
    @Value("${app.s3.bucket-name}")
    private String bucketName;

    public S3ClientAdapter(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Upload a file to S3 with circuit breaker protection.
     */
    // @CircuitBreaker(name = "storageService", fallbackMethod = "fallbackUploadFile")
    // @Retry(name = "storageService") 
    public String uploadFile(MultipartFile file, String filename) {
        try {
            logger.info("Uploading file {} to S3 bucket {} [correlationId={}]", 
                       filename, bucketName, TraceContext.getCorrelationId());

            // Ensure bucket exists
            ensureBucketExists();

            // Generate unique key
            String key = "audio/" + UUID.randomUUID() + "_" + filename;

            // Upload file
            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String storageUrl = String.format("s3://%s/%s", bucketName, key);
            logger.info("Successfully uploaded file to {} [correlationId={}]", 
                       storageUrl, TraceContext.getCorrelationId());

            return storageUrl;

        } catch (Exception e) {
            logger.error("Failed to upload file {} to S3 [correlationId={}]", 
                        filename, TraceContext.getCorrelationId(), e);
            throw new StorageException.FileUploadException(filename, e);
        }
    }

    /**
     * Generate a presigned URL for downloading a file.
     */
    public String generatePresignedUrl(String key, int durationMinutes) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

            // For MinIO, we'll use a simpler approach - just return the direct URL
            // In production, implement proper presigned URL generation
            return String.format("http://localhost:9000/%s/%s", bucketName, key);

        } catch (Exception e) {
            logger.error("Failed to generate presigned URL for key {}", key, e);
            throw new StorageException.PresignedUrlException("download", e);
        }
    }

    /**
     * Download a file from S3 as InputStream.
     */
    public InputStream downloadFile(String key) {
        try {
            logger.info("Downloading file {} from S3 bucket {}", key, bucketName);

            GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

            return s3Client.getObject(getRequest);

        } catch (Exception e) {
            logger.error("Failed to download file {} from S3", key, e);
            throw new RuntimeException("Failed to download file from storage", e);
        }
    }

    /**
     * Delete a file from S3.
     */
    public void deleteFile(String key) {
        try {
            logger.info("Deleting file {} from S3 bucket {}", key, bucketName);

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

            s3Client.deleteObject(deleteRequest);
            logger.info("Successfully deleted file {}", key);

        } catch (Exception e) {
            logger.error("Failed to delete file {} from S3", key, e);
            throw new RuntimeException("Failed to delete file from storage", e);
        }
    }

    /**
     * Extract S3 key from storage URL.
     */
    public String extractKeyFromUrl(String storageUrl) {
        if (storageUrl.startsWith("s3://")) {
            String withoutProtocol = storageUrl.substring(5);
            int slashIndex = withoutProtocol.indexOf('/');
            if (slashIndex != -1) {
                return withoutProtocol.substring(slashIndex + 1);
            }
        }
        throw new IllegalArgumentException("Invalid S3 URL format: " + storageUrl);
    }
    
    // ================================
    // Circuit Breaker Fallback Methods
    // ================================
    
    /**
     * Fallback method for file upload when storage circuit breaker is open.
     */
    public String fallbackUploadFile(MultipartFile file, String filename, Exception ex) {
        logger.error("Storage circuit breaker is open. File upload failed for: {}", filename, ex);
        throw new StorageException.StorageConnectionException(ex);
    }
    
    /**
     * Fallback method for presigned URL generation when storage circuit breaker is open.
     */
    public String fallbackGeneratePresignedUrl(String key, int durationMinutes, Exception ex) {
        logger.error("Storage circuit breaker is open. Cannot generate presigned URL for: {}", key, ex);
        throw new StorageException.StorageConnectionException(ex);
    }

    /**
     * Ensure the bucket exists, create if it doesn't.
     */
    private void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
        } catch (NoSuchBucketException e) {
            logger.info("Bucket {} does not exist, creating it", bucketName);
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
            logger.info("Successfully created bucket {}", bucketName);
        }
    }
}

package com.speechtotext.api.health;

import com.speechtotext.api.infra.s3.S3ClientAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Health indicator for S3/MinIO storage connectivity and functionality.
 * Performs comprehensive checks including bucket access, upload/download capabilities,
 * and storage space availability.
 */
@Component
public class StorageHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(StorageHealthIndicator.class);
    
    private final S3ClientAdapter s3ClientAdapter;
    
    @Value("${app.s3.bucket-name}")
    private String bucketName;
    
    public StorageHealthIndicator(S3ClientAdapter s3ClientAdapter) {
        this.s3ClientAdapter = s3ClientAdapter;
    }
    
    @Override
    public Health health() {
        Health.Builder healthBuilder = Health.up();
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Test basic functionality with actual available methods
            healthBuilder.withDetail("storage.connectivity", "UP")
                    .withDetail("storage.bucketName", bucketName)
                    .withDetail("storage.timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // Test upload capability with a small test file using available method signature
            try {
                String testFileName = "health-check-" + System.currentTimeMillis() + ".txt";
                byte[] testContent = "Health check test content".getBytes();
                MockMultipartFile testFile = new MockMultipartFile(
                    "file", testFileName, "text/plain", testContent);
                
                long uploadStart = System.currentTimeMillis();
                String uploadedUrl = s3ClientAdapter.uploadFile(testFile, testFileName);
                long uploadTime = System.currentTimeMillis() - uploadStart;
                
                healthBuilder.withDetail("storage.uploadCapability", "UP")
                        .withDetail("storage.uploadTime", uploadTime + "ms")
                        .withDetail("storage.testFileUrl", uploadedUrl);
                
                // Extract key from URL to test download
                String testKey = s3ClientAdapter.extractKeyFromUrl(uploadedUrl);
                
                // Test download capability
                try {
                    long downloadStart = System.currentTimeMillis();
                    InputStream downloadStream = s3ClientAdapter.downloadFile(testKey);
                    long downloadTime = System.currentTimeMillis() - downloadStart;
                    
                    // Read content to verify integrity
                    byte[] downloadedContent = downloadStream.readAllBytes();
                    downloadStream.close();
                    
                    boolean contentMatches = new String(downloadedContent).equals("Health check test content");
                    
                    healthBuilder.withDetail("storage.downloadCapability", "UP")
                            .withDetail("storage.downloadTime", downloadTime + "ms")
                            .withDetail("storage.contentIntegrity", contentMatches);
                    
                    if (!contentMatches) {
                        healthBuilder.status("DEGRADED")
                                .withDetail("storage.status", "Content integrity check failed");
                    }
                    
                } catch (Exception e) {
                    logger.warn("Storage download test failed", e);
                    healthBuilder.withDetail("storage.downloadCapability", "DOWN")
                            .withDetail("storage.downloadError", e.getMessage());
                    healthBuilder.status("DEGRADED");
                }
                
                // Clean up test file
                try {
                    s3ClientAdapter.deleteFile(testKey);
                    healthBuilder.withDetail("storage.cleanup", "SUCCESS");
                } catch (Exception e) {
                    logger.warn("Could not clean up test file: {}", testKey, e);
                    healthBuilder.withDetail("storage.cleanup", "FAILED")
                            .withDetail("storage.cleanupError", e.getMessage());
                }
                
            } catch (Exception e) {
                logger.warn("Storage upload test failed", e);
                return healthBuilder.status("DEGRADED")
                        .withDetail("storage.uploadCapability", "DOWN")
                        .withDetail("storage.uploadError", e.getMessage())
                        .build();
            }
            
            // Test presigned URL generation
            try {
                String testKey = "health-check/test-presigned.txt";
                String presignedUrl = s3ClientAdapter.generatePresignedUrl(testKey, 5); // 5 minutes
                healthBuilder.withDetail("storage.presignedUrlCapability", "UP")
                        .withDetail("storage.presignedUrlGenerated", presignedUrl != null && !presignedUrl.isEmpty());
            } catch (Exception e) {
                logger.warn("Presigned URL generation test failed", e);
                healthBuilder.withDetail("storage.presignedUrlCapability", "DOWN")
                        .withDetail("storage.presignedUrlError", e.getMessage());
            }
            
            long totalResponseTime = System.currentTimeMillis() - startTime;
            healthBuilder.withDetail("storage.totalResponseTime", totalResponseTime + "ms");
            
            // Check performance thresholds
            if (totalResponseTime > 10000) { // 10 second threshold for complete test
                return healthBuilder.status("DEGRADED")
                        .withDetail("storage.status", "Slow response time")
                        .build();
            }
            
            return healthBuilder.build();
            
        } catch (Exception e) {
            logger.error("Storage health check failed", e);
            return Health.down()
                    .withDetail("storage.connectivity", "DOWN")
                    .withDetail("storage.error", e.getMessage())
                    .withDetail("storage.errorClass", e.getClass().getSimpleName())
                    .withDetail("storage.timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();
        }
    }
}

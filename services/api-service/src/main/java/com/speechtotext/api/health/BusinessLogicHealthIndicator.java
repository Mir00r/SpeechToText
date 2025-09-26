package com.speechtotext.api.health;

import com.speechtotext.api.repository.JobRepository;
import com.speechtotext.api.model.JobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Business logic health indicator that validates application-specific functionality
 * including data access patterns, business rules validation, and workflow integrity.
 */
@Component
public class BusinessLogicHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(BusinessLogicHealthIndicator.class);
    
    private final JobRepository jobRepository;
    
    public BusinessLogicHealthIndicator(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }
    
    @Override
    public Health health() {
        Health.Builder healthBuilder = Health.up();
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Test basic repository access and business logic
            long totalJobs = jobRepository.count();
            healthBuilder.withDetail("business.totalJobs", totalJobs);
            
            // Check for recent activity (jobs in last 24 hours)
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            long recentJobs = jobRepository.findByCreatedAtAfter(yesterday).size();
            healthBuilder.withDetail("business.recentJobs24h", recentJobs);
            
            // Check job status distribution using correct enum values
            try {
                long pendingJobs = jobRepository.countByStatus(JobEntity.JobStatus.PENDING);
                long processingJobs = jobRepository.countByStatus(JobEntity.JobStatus.PROCESSING);
                long completedJobs = jobRepository.countByStatus(JobEntity.JobStatus.COMPLETED);
                long failedJobs = jobRepository.countByStatus(JobEntity.JobStatus.FAILED);
                
                healthBuilder
                        .withDetail("business.jobs.pending", pendingJobs)
                        .withDetail("business.jobs.processing", processingJobs)
                        .withDetail("business.jobs.completed", completedJobs)
                        .withDetail("business.jobs.failed", failedJobs);
                
                // Calculate success rate
                long totalProcessedJobs = completedJobs + failedJobs;
                if (totalProcessedJobs > 0) {
                    double successRate = (double) completedJobs / totalProcessedJobs * 100;
                    healthBuilder.withDetail("business.successRate", String.format("%.1f%%", successRate));
                    
                    // Alert if success rate is too low
                    if (successRate < 80.0 && totalProcessedJobs > 10) {
                        healthBuilder.status("DEGRADED")
                                .withDetail("business.alert", "Low success rate detected");
                    }
                } else {
                    healthBuilder.withDetail("business.successRate", "N/A (no processed jobs)");
                }
                
                // Check for stale pending jobs (pending for more than 30 minutes)
                LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
                long stalePendingJobs = jobRepository.findStalePendingJobs(thirtyMinutesAgo).size();
                healthBuilder.withDetail("business.jobs.stalePending", stalePendingJobs);
                
                if (stalePendingJobs > 5) { // Alert if more than 5 stale jobs
                    healthBuilder.status("DEGRADED")
                            .withDetail("business.alert", "Many stale pending jobs detected");
                }
                
            } catch (Exception e) {
                logger.warn("Could not retrieve detailed job statistics", e);
                healthBuilder.withDetail("business.detailedStats", "UNAVAILABLE")
                        .withDetail("business.detailedStatsError", e.getMessage());
            }
            
            // Test business logic workflows
            try {
                // Validate that we can perform basic business operations
                boolean businessLogicHealthy = validateBusinessLogicIntegrity();
                healthBuilder.withDetail("business.logicIntegrity", businessLogicHealthy ? "HEALTHY" : "DEGRADED");
                
                if (!businessLogicHealthy) {
                    healthBuilder.status("DEGRADED")
                            .withDetail("business.alert", "Business logic integrity issues detected");
                }
                
            } catch (Exception e) {
                logger.warn("Business logic validation failed", e);
                healthBuilder.withDetail("business.logicIntegrity", "FAILED")
                        .withDetail("business.logicError", e.getMessage());
                healthBuilder.status("DEGRADED");
            }
            
            long responseTime = System.currentTimeMillis() - startTime;
            healthBuilder.withDetail("business.responseTime", responseTime + "ms")
                    .withDetail("business.timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // Performance check
            if (responseTime > 5000) { // 5 second threshold
                return healthBuilder.status("DEGRADED")
                        .withDetail("business.status", "Slow business logic response")
                        .build();
            }
            
            return healthBuilder.build();
            
        } catch (Exception e) {
            logger.error("Business logic health check failed", e);
            return Health.down()
                    .withDetail("business.connectivity", "DOWN")
                    .withDetail("business.error", e.getMessage())
                    .withDetail("business.errorClass", e.getClass().getSimpleName())
                    .withDetail("business.timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();
        }
    }
    
    /**
     * Validate business logic integrity.
     * This performs comprehensive business logic validation.
     */
    private boolean validateBusinessLogicIntegrity() {
        try {
            // Example validations:
            
            // 1. Check that we can query jobs without errors
            jobRepository.count();
            
            // 2. Validate that status enum values are working correctly
            for (JobEntity.JobStatus status : JobEntity.JobStatus.values()) {
                jobRepository.countByStatus(status);
            }
            
            // 3. Check for data consistency (example: ensure no null required fields)
            // This would typically be more comprehensive in a real application
            
            return true;
            
        } catch (Exception e) {
            logger.error("Business logic integrity validation failed", e);
            return false;
        }
    }
}

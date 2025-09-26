package com.speechtotext.api.controller;

import com.speechtotext.api.client.TranscriptionServiceClient;
import com.speechtotext.api.infra.s3.S3ClientAdapter;
import com.speechtotext.api.repository.JobRepository;
import com.speechtotext.api.model.JobEntity;
import com.speechtotext.api.service.CircuitBreakerMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced health check controller providing comprehensive system health information
 * beyond the standard Spring Boot Actuator endpoints.
 */
@RestController
@RequestMapping("/internal/v1/health")
public class EnhancedHealthController {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedHealthController.class);
    
    private final JdbcTemplate jdbcTemplate;
    private final S3ClientAdapter s3ClientAdapter;
    private final TranscriptionServiceClient transcriptionServiceClient;
    private final JobRepository jobRepository;
    private final CircuitBreakerMonitorService circuitBreakerMonitorService;
    
    @Value("${app.s3.bucket-name}")
    private String bucketName;
    
    public EnhancedHealthController(
            JdbcTemplate jdbcTemplate,
            S3ClientAdapter s3ClientAdapter,
            TranscriptionServiceClient transcriptionServiceClient,
            JobRepository jobRepository,
            CircuitBreakerMonitorService circuitBreakerMonitorService) {
        this.jdbcTemplate = jdbcTemplate;
        this.s3ClientAdapter = s3ClientAdapter;
        this.transcriptionServiceClient = transcriptionServiceClient;
        this.jobRepository = jobRepository;
        this.circuitBreakerMonitorService = circuitBreakerMonitorService;
    }
    
    /**
     * Comprehensive health check endpoint providing detailed system status.
     */
    @GetMapping("/comprehensive")
    public ResponseEntity<Map<String, Object>> getComprehensiveHealth() {
        Map<String, Object> healthResponse = new HashMap<>();
        String overallStatus = "UP";
        
        try {
            // Database health
            Map<String, Object> dbHealth = checkDatabaseHealth();
            healthResponse.put("database", dbHealth);
            if (!"UP".equals(dbHealth.get("status"))) {
                if ("DOWN".equals(dbHealth.get("status"))) {
                    overallStatus = "DOWN";
                } else if ("DEGRADED".equals(overallStatus) && "DEGRADED".equals(dbHealth.get("status"))) {
                    overallStatus = "DEGRADED";
                }
            }
            
            // Storage health
            Map<String, Object> storageHealth = checkStorageHealth();
            healthResponse.put("storage", storageHealth);
            if (!"UP".equals(storageHealth.get("status"))) {
                if ("DOWN".equals(storageHealth.get("status"))) {
                    overallStatus = "DOWN";
                } else if (!"DOWN".equals(overallStatus) && "DEGRADED".equals(storageHealth.get("status"))) {
                    overallStatus = "DEGRADED";
                }
            }
            
            // External service health
            Map<String, Object> externalServiceHealth = checkExternalServiceHealth();
            healthResponse.put("externalServices", externalServiceHealth);
            if (!"UP".equals(externalServiceHealth.get("status"))) {
                if ("DOWN".equals(externalServiceHealth.get("status"))) {
                    // External service down is degraded, not full system down
                    if (!"DOWN".equals(overallStatus)) {
                        overallStatus = "DEGRADED";
                    }
                }
            }
            
            // Circuit breaker health
            Map<String, Object> circuitBreakerHealth = checkCircuitBreakerHealth();
            healthResponse.put("circuitBreaker", circuitBreakerHealth);
            
            // Business logic health
            Map<String, Object> businessHealth = checkBusinessLogicHealth();
            healthResponse.put("business", businessHealth);
            if (!"UP".equals(businessHealth.get("status"))) {
                if ("DOWN".equals(businessHealth.get("status"))) {
                    overallStatus = "DOWN";
                } else if (!"DOWN".equals(overallStatus) && "DEGRADED".equals(businessHealth.get("status"))) {
                    overallStatus = "DEGRADED";
                }
            }
            
            // System resources health
            Map<String, Object> systemHealth = checkSystemResourcesHealth();
            healthResponse.put("system", systemHealth);
            if (!"UP".equals(systemHealth.get("status"))) {
                if ("DOWN".equals(systemHealth.get("status"))) {
                    overallStatus = "DOWN";
                } else if (!"DOWN".equals(overallStatus) && "DEGRADED".equals(systemHealth.get("status"))) {
                    overallStatus = "DEGRADED";
                }
            }
            
            // Overall status
            healthResponse.put("status", overallStatus);
            healthResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            healthResponse.put("service", "speech-to-text-api");
            healthResponse.put("version", getClass().getPackage().getImplementationVersion());
            
            // Return appropriate HTTP status
            if ("DOWN".equals(overallStatus)) {
                return ResponseEntity.status(503).body(healthResponse);
            } else if ("DEGRADED".equals(overallStatus)) {
                return ResponseEntity.status(200).body(healthResponse); // Still operational
            } else {
                return ResponseEntity.ok(healthResponse);
            }
            
        } catch (Exception e) {
            logger.error("Comprehensive health check failed", e);
            healthResponse.put("status", "DOWN");
            healthResponse.put("error", e.getMessage());
            healthResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            return ResponseEntity.status(503).body(healthResponse);
        }
    }
    
    /**
     * Quick health check for load balancer probes.
     */
    @GetMapping("/quick")
    public ResponseEntity<Map<String, Object>> getQuickHealth() {
        Map<String, Object> healthResponse = new HashMap<>();
        
        try {
            // Just check database connectivity for quick check
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            
            healthResponse.put("status", "UP");
            healthResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            return ResponseEntity.ok(healthResponse);
            
        } catch (Exception e) {
            logger.error("Quick health check failed", e);
            healthResponse.put("status", "DOWN");
            healthResponse.put("error", e.getMessage());
            healthResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            return ResponseEntity.status(503).body(healthResponse);
        }
    }
    
    private Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            long startTime = System.currentTimeMillis();
            String result = jdbcTemplate.queryForObject("SELECT 'OK'", String.class);
            long responseTime = System.currentTimeMillis() - startTime;
            
            health.put("status", "UP");
            health.put("responseTime", responseTime + "ms");
            health.put("connectivity", result);
            
            // Check job table access
            try {
                Long jobCountResult = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transcription_jobs", Long.class);
                long jobCount = jobCountResult != null ? jobCountResult : 0;
                health.put("jobTableAccessible", true);
                health.put("totalJobs", jobCount);
            } catch (Exception e) {
                health.put("jobTableAccessible", false);
                health.put("jobTableError", e.getMessage());
            }
            
            if (responseTime > 1000) {
                health.put("status", "DEGRADED");
            }
            
        } catch (DataAccessException e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        
        return health;
    }
    
    private Map<String, Object> checkStorageHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            health.put("status", "UP");
            health.put("bucketName", bucketName);
            
            // Test presigned URL generation as a basic connectivity test
            String testUrl = s3ClientAdapter.generatePresignedUrl("health-test", 5);
            health.put("presignedUrlCapability", testUrl != null && !testUrl.isEmpty());
            
        } catch (Exception e) {
            health.put("status", "DEGRADED");
            health.put("error", e.getMessage());
        }
        
        return health;
    }
    
    private Map<String, Object> checkExternalServiceHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            long startTime = System.currentTimeMillis();
            boolean isHealthy = transcriptionServiceClient.isTranscriptionServiceHealthy();
            long responseTime = System.currentTimeMillis() - startTime;
            
            health.put("status", isHealthy ? "UP" : "DOWN");
            health.put("responseTime", responseTime + "ms");
            health.put("transcriptionService", isHealthy);
            
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        
        return health;
    }
    
    private Map<String, Object> checkCircuitBreakerHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            String transcriptionServiceState = circuitBreakerMonitorService.getTranscriptionServiceCircuitBreakerState();
            CircuitBreakerMonitorService.CircuitBreakerMetrics metrics = 
                circuitBreakerMonitorService.getTranscriptionServiceMetrics();
            
            health.put("status", "OPEN".equals(transcriptionServiceState) ? "DEGRADED" : "UP");
            health.put("transcriptionServiceState", transcriptionServiceState);
            health.put("failureRate", String.format("%.2f%%", metrics.failureRate()));
            health.put("successfulCalls", metrics.successfulCalls());
            health.put("failedCalls", metrics.failedCalls());
            
        } catch (Exception e) {
            health.put("status", "UNKNOWN");
            health.put("error", e.getMessage());
        }
        
        return health;
    }
    
    private Map<String, Object> checkBusinessLogicHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            long totalJobs = jobRepository.count();
            long pendingJobs = jobRepository.countByStatus(JobEntity.JobStatus.PENDING);
            long completedJobs = jobRepository.countByStatus(JobEntity.JobStatus.COMPLETED);
            long failedJobs = jobRepository.countByStatus(JobEntity.JobStatus.FAILED);
            
            health.put("status", "UP");
            health.put("totalJobs", totalJobs);
            health.put("pendingJobs", pendingJobs);
            health.put("completedJobs", completedJobs);
            health.put("failedJobs", failedJobs);
            
            // Calculate success rate
            long processedJobs = completedJobs + failedJobs;
            if (processedJobs > 0) {
                double successRate = (double) completedJobs / processedJobs * 100;
                health.put("successRate", String.format("%.1f%%", successRate));
                
                if (successRate < 80.0 && processedJobs > 10) {
                    health.put("status", "DEGRADED");
                }
            }
            
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        
        return health;
    }
    
    private Map<String, Object> checkSystemResourcesHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            double heapUsageRatio = (double) heapUsed / heapMax;
            
            File rootPath = new File("/");
            long totalSpace = rootPath.getTotalSpace();
            long freeSpace = rootPath.getFreeSpace();
            double diskUsageRatio = (double) (totalSpace - freeSpace) / totalSpace;
            
            health.put("status", "UP");
            health.put("heapUsage", String.format("%.1f%%", heapUsageRatio * 100));
            health.put("diskUsage", String.format("%.1f%%", diskUsageRatio * 100));
            health.put("processors", Runtime.getRuntime().availableProcessors());
            
            if (heapUsageRatio >= 0.9 || diskUsageRatio >= 0.9) {
                health.put("status", "DOWN");
            } else if (heapUsageRatio >= 0.8 || diskUsageRatio >= 0.8) {
                health.put("status", "DEGRADED");
            }
            
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        
        return health;
    }
}

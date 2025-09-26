package com.speechtotext.api.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Enhanced database health indicator that performs comprehensive database connectivity
 * and performance checks including connection pool status, query performance, and
 * database metadata.
 */
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthIndicator.class);
    
    private final JdbcTemplate jdbcTemplate;
    
    public DatabaseHealthIndicator(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public Health health() {
        Health.Builder healthBuilder = Health.up();
        
        try {
            // Basic connectivity test
            long startTime = System.currentTimeMillis();
            String result = jdbcTemplate.queryForObject("SELECT 'Database Connection OK'", String.class);
            long responseTime = System.currentTimeMillis() - startTime;
            
            healthBuilder.withDetail("database.connectivity", "UP")
                    .withDetail("database.responseTime", responseTime + "ms")
                    .withDetail("database.testQuery", result)
                    .withDetail("database.timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // Check database version and basic info
            try {
                String version = jdbcTemplate.queryForObject("SELECT version()", String.class);
                if (version != null) {
                    // Extract just the PostgreSQL version number
                    String shortVersion = version.split(" ")[0] + " " + version.split(" ")[1];
                    healthBuilder.withDetail("database.version", shortVersion);
                }
            } catch (Exception e) {
                logger.debug("Could not retrieve database version", e);
                healthBuilder.withDetail("database.version", "Unknown");
            }
            
            // Check connection pool status (if available)
            try {
                Integer activeConnections = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM pg_stat_activity WHERE state = 'active'", Integer.class);
                healthBuilder.withDetail("database.activeConnections", activeConnections);
            } catch (Exception e) {
                logger.debug("Could not retrieve active connections count", e);
            }
            
            // Test a simple query on our main tables to ensure they're accessible
            try {
                Integer jobCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM transcription_jobs", Integer.class);
                healthBuilder.withDetail("database.jobTableAccessible", true)
                        .withDetail("database.totalJobs", jobCount);
            } catch (Exception e) {
                logger.warn("Could not access transcription_jobs table", e);
                healthBuilder.withDetail("database.jobTableAccessible", false)
                        .withDetail("database.jobTableError", e.getMessage());
            }
            
            // Performance check - if response time is too high, mark as degraded
            if (responseTime > 1000) { // 1 second threshold
                return healthBuilder.status("DEGRADED")
                        .withDetail("database.status", "Slow response time")
                        .build();
            }
            
            return healthBuilder.build();
            
        } catch (DataAccessException e) {
            logger.error("Database health check failed", e);
            return Health.down()
                    .withDetail("database.connectivity", "DOWN")
                    .withDetail("database.error", e.getMessage())
                    .withDetail("database.errorClass", e.getClass().getSimpleName())
                    .withDetail("database.timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();
        } catch (Exception e) {
            logger.error("Unexpected error during database health check", e);
            return Health.down()
                    .withDetail("database.connectivity", "DOWN")
                    .withDetail("database.error", "Unexpected error: " + e.getMessage())
                    .withDetail("database.timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();
        }
    }
}

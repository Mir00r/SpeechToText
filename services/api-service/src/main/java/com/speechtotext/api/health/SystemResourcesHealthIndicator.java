package com.speechtotext.api.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Health indicator for system resources including memory, disk space,
 * and JVM metrics. Provides detailed information about system health
 * and resource utilization.
 */
@Component
public class SystemResourcesHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(SystemResourcesHealthIndicator.class);
    
    // Thresholds for resource warnings
    private static final double MEMORY_WARNING_THRESHOLD = 0.8; // 80%
    private static final double MEMORY_CRITICAL_THRESHOLD = 0.9; // 90%
    private static final double DISK_WARNING_THRESHOLD = 0.8; // 80%
    private static final double DISK_CRITICAL_THRESHOLD = 0.9; // 90%
    
    @Override
    public Health health() {
        Health.Builder healthBuilder = Health.up();
        
        try {
            // Memory information
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
            long nonHeapMax = memoryBean.getNonHeapMemoryUsage().getMax();
            
            double heapUsageRatio = (double) heapUsed / heapMax;
            double nonHeapUsageRatio = nonHeapMax > 0 ? (double) nonHeapUsed / nonHeapMax : 0;
            
            healthBuilder
                    .withDetail("memory.heap.used", formatBytes(heapUsed))
                    .withDetail("memory.heap.max", formatBytes(heapMax))
                    .withDetail("memory.heap.usagePercentage", String.format("%.1f%%", heapUsageRatio * 100))
                    .withDetail("memory.nonHeap.used", formatBytes(nonHeapUsed))
                    .withDetail("memory.nonHeap.max", nonHeapMax > 0 ? formatBytes(nonHeapMax) : "unlimited")
                    .withDetail("memory.nonHeap.usagePercentage", nonHeapMax > 0 ? String.format("%.1f%%", nonHeapUsageRatio * 100) : "N/A");
            
            // JVM Runtime information
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            long uptime = runtimeBean.getUptime();
            String jvmVersion = runtimeBean.getVmVersion();
            String javaVersion = System.getProperty("java.version");
            
            healthBuilder
                    .withDetail("jvm.uptime", formatDuration(uptime))
                    .withDetail("jvm.version", jvmVersion)
                    .withDetail("java.version", javaVersion)
                    .withDetail("processors.available", Runtime.getRuntime().availableProcessors());
            
            // Disk space information
            File rootPath = new File("/");
            long totalSpace = rootPath.getTotalSpace();
            long freeSpace = rootPath.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;
            double diskUsageRatio = (double) usedSpace / totalSpace;
            
            healthBuilder
                    .withDetail("disk.total", formatBytes(totalSpace))
                    .withDetail("disk.free", formatBytes(freeSpace))
                    .withDetail("disk.used", formatBytes(usedSpace))
                    .withDetail("disk.usagePercentage", String.format("%.1f%%", diskUsageRatio * 100));
            
            // Thread information
            int activeThreads = Thread.activeCount();
            healthBuilder.withDetail("threads.active", activeThreads);
            
            // Timestamp
            healthBuilder.withDetail("system.timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // Determine health status based on resource usage
            String status = "UP";
            String statusReason = "All resources within normal limits";
            
            if (heapUsageRatio >= MEMORY_CRITICAL_THRESHOLD || diskUsageRatio >= DISK_CRITICAL_THRESHOLD) {
                status = "DOWN";
                statusReason = "Critical resource usage detected";
                healthBuilder.withDetail("system.criticalIssues", "Memory or disk usage is critically high");
            } else if (heapUsageRatio >= MEMORY_WARNING_THRESHOLD || diskUsageRatio >= DISK_WARNING_THRESHOLD) {
                status = "DEGRADED";
                statusReason = "High resource usage detected";
                healthBuilder.withDetail("system.warnings", "Memory or disk usage is high");
            }
            
            healthBuilder
                    .withDetail("system.status", status)
                    .withDetail("system.statusReason", statusReason);
            
            if ("DOWN".equals(status)) {
                return healthBuilder.down().build();
            } else if ("DEGRADED".equals(status)) {
                return healthBuilder.status("DEGRADED").build();
            } else {
                return healthBuilder.build();
            }
            
        } catch (Exception e) {
            logger.error("System resources health check failed", e);
            return Health.down()
                    .withDetail("system.error", e.getMessage())
                    .withDetail("system.errorClass", e.getClass().getSimpleName())
                    .withDetail("system.timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();
        }
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %cB", bytes / Math.pow(1024, exp), pre);
    }
    
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
}

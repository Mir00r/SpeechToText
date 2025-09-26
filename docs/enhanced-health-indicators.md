# Enhanced Health Check Indicators

## Overview

The Speech to Text service includes comprehensive health check indicators that provide detailed monitoring capabilities beyond the standard Spring Boot Actuator endpoints. These health indicators monitor all critical components of the system and provide actionable health information for operations teams.

## Health Indicator Components

### 1. Database Health Indicator
**Endpoint**: `/actuator/health` (component: `database`)  
**Purpose**: Monitors PostgreSQL database connectivity, performance, and data integrity

#### Checks Performed:
- **Basic Connectivity**: Simple query execution test
- **Response Time**: Database query performance monitoring
- **Version Information**: PostgreSQL version detection
- **Connection Pool Status**: Active connections monitoring (when available)
- **Table Access**: Validates access to critical application tables
- **Performance Thresholds**: Alerts when response time exceeds 1 second

#### Health Statuses:
- **UP**: Database responding normally with acceptable performance
- **DEGRADED**: Database responding but with slow performance (>1 second)
- **DOWN**: Database connectivity issues or query failures

#### Example Response:
```json
{
  "database": {
    "status": "UP",
    "details": {
      "connectivity": "UP",
      "responseTime": "45ms",
      "version": "PostgreSQL 15.3",
      "activeConnections": 5,
      "jobTableAccessible": true,
      "totalJobs": 1247,
      "timestamp": "2025-09-26T10:30:15"
    }
  }
}
```

### 2. Storage Health Indicator  
**Endpoint**: `/actuator/health` (component: `storage`)  
**Purpose**: Monitors S3/MinIO storage connectivity and functionality

#### Checks Performed:
- **Bucket Access**: Verifies configured bucket exists and is accessible
- **Upload Capability**: Tests file upload with small test file
- **Download Capability**: Tests file download and content integrity
- **Presigned URL Generation**: Validates URL generation capability
- **Content Integrity**: Verifies uploaded/downloaded content matches
- **Cleanup Operations**: Tests file deletion capability
- **Performance Monitoring**: Tracks operation response times

#### Health Statuses:
- **UP**: All storage operations functioning normally
- **DEGRADED**: Storage accessible but with issues (slow performance, partial failures)
- **DOWN**: Storage connectivity issues or critical operation failures

#### Example Response:
```json
{
  "storage": {
    "status": "UP",
    "details": {
      "connectivity": "UP",
      "bucketName": "speechtotext-audio",
      "uploadCapability": "UP",
      "uploadTime": "234ms",
      "downloadCapability": "UP",
      "downloadTime": "156ms",
      "contentIntegrity": true,
      "presignedUrlCapability": "UP",
      "cleanup": "SUCCESS",
      "totalResponseTime": "456ms",
      "timestamp": "2025-09-26T10:30:15"
    }
  }
}
```

### 3. Transcription Service Health Indicator
**Endpoint**: `/actuator/health` (component: `transcriptionService`)  
**Purpose**: Monitors external transcription service connectivity and capability

#### Checks Performed:
- **Service Connectivity**: Basic health endpoint check
- **Response Time Monitoring**: Performance tracking
- **Capability Validation**: Extended service functionality tests
- **Performance Assessment**: Response time categorization

#### Health Statuses:
- **UP**: Transcription service healthy and responsive
- **DEGRADED**: Service responding but with performance issues
- **DOWN**: Service unavailable or health check failing

#### Example Response:
```json
{
  "transcriptionService": {
    "status": "UP",
    "details": {
      "connectivity": "UP",
      "responseTime": "850ms",
      "capabilities": "BASIC_HEALTH_OK",
      "performance": "NORMAL",
      "timestamp": "2025-09-26T10:30:15"
    }
  }
}
```

### 4. System Resources Health Indicator
**Endpoint**: `/actuator/health` (component: `systemResources`)  
**Purpose**: Monitors JVM and system resource utilization

#### Checks Performed:
- **Memory Usage**: Heap and non-heap memory monitoring
- **JVM Information**: Runtime version, uptime, processor count
- **Disk Space**: Root filesystem usage monitoring
- **Thread Count**: Active thread monitoring
- **Performance Thresholds**: Resource usage alerts

#### Thresholds:
- **Memory Warning**: 80% heap usage
- **Memory Critical**: 90% heap usage  
- **Disk Warning**: 80% disk usage
- **Disk Critical**: 90% disk usage

#### Health Statuses:
- **UP**: All resources within normal limits
- **DEGRADED**: High resource usage (warning thresholds exceeded)
- **DOWN**: Critical resource usage (critical thresholds exceeded)

#### Example Response:
```json
{
  "system": {
    "status": "UP",
    "details": {
      "memory": {
        "heap": {
          "used": "512.3 MB",
          "max": "2.0 GB",
          "usagePercentage": "25.6%"
        },
        "nonHeap": {
          "used": "89.7 MB",
          "max": "unlimited",
          "usagePercentage": "N/A"
        }
      },
      "jvm": {
        "uptime": "2d 14h 32m",
        "version": "OpenJDK 21.0.1",
        "processors": 8
      },
      "disk": {
        "total": "100.0 GB",
        "free": "67.8 GB",
        "used": "32.2 GB",
        "usagePercentage": "32.2%"
      },
      "threads": {
        "active": 45
      },
      "status": "UP",
      "statusReason": "All resources within normal limits",
      "timestamp": "2025-09-26T10:30:15"
    }
  }
}
```

### 5. Circuit Breaker Health Indicator (Enhanced)
**Endpoint**: `/actuator/health` (component: `circuitBreakers`)  
**Purpose**: Monitors circuit breaker states and provides fault tolerance status

#### Checks Performed:
- **Circuit Breaker States**: Current state of all circuit breakers
- **Failure Rate Monitoring**: Real-time failure rate tracking
- **Call Statistics**: Success/failure/slow call counts
- **Performance Metrics**: Detailed circuit breaker metrics

#### Health Statuses:
- **UP**: All circuit breakers closed with acceptable failure rates
- **DEGRADED**: High failure rates but circuit breakers still closed, or open circuits with working fallbacks
- **DOWN**: Circuit breakers open indicating service unavailability

#### Example Response:
```json
{
  "circuitBreaker": {
    "status": "UP",
    "details": {
      "transcriptionService": {
        "state": "CLOSED",
        "failureRate": "2.3%",
        "slowCallRate": "1.1%",
        "successfulCalls": 1847,
        "failedCalls": 43,
        "slowCalls": 21,
        "notPermittedCalls": 0
      },
      "storageService": {
        "state": "CLOSED",
        "failureRate": "0.8%",
        "successfulCalls": 2156,
        "failedCalls": 17
      },
      "timestamp": "2025-09-26T10:30:15"
    }
  }
}
```

### 6. Business Logic Health Indicator
**Endpoint**: `/actuator/health` (component: `business`)  
**Purpose**: Validates application-specific business logic and workflow integrity

#### Checks Performed:
- **Total Job Statistics**: Overall job counts and trends
- **Recent Activity**: Activity monitoring (24-hour window)
- **Job Status Distribution**: Breakdown by job status
- **Success Rate Calculation**: Processing success percentage
- **Stale Job Detection**: Identifies stuck or orphaned jobs
- **Data Integrity Validation**: Business rule compliance checks

#### Health Statuses:
- **UP**: Business logic functioning normally with good success rates
- **DEGRADED**: Business logic issues detected (low success rate, stale jobs)
- **DOWN**: Critical business logic failures or data access issues

#### Example Response:
```json
{
  "business": {
    "status": "UP",
    "details": {
      "totalJobs": 15647,
      "recentJobs24h": 423,
      "jobs": {
        "pending": 23,
        "processing": 8,
        "completed": 14567,
        "failed": 1049
      },
      "successRate": "93.3%",
      "stalePending": 1,
      "logicIntegrity": "HEALTHY",
      "responseTime": "89ms",
      "timestamp": "2025-09-26T10:30:15"
    }
  }
}
```

## Enhanced Health Endpoints

### 1. Comprehensive Health Check
**Endpoint**: `GET /internal/v1/health/comprehensive`  
**Purpose**: Provides complete system health status across all components

#### Response Format:
```json
{
  "status": "UP",
  "database": { /* Database health details */ },
  "storage": { /* Storage health details */ },
  "externalServices": { /* External service health */ },
  "circuitBreaker": { /* Circuit breaker health */ },
  "business": { /* Business logic health */ },
  "system": { /* System resources health */ },
  "timestamp": "2025-09-26T10:30:15",
  "service": "speech-to-text-api",
  "version": "1.0.0"
}
```

#### HTTP Status Codes:
- **200 OK**: System healthy or degraded but operational
- **503 Service Unavailable**: System down or critical issues

### 2. Quick Health Check
**Endpoint**: `GET /internal/v1/health/quick`  
**Purpose**: Fast health check for load balancer probes

#### Response Format:
```json
{
  "status": "UP",
  "timestamp": "2025-09-26T10:30:15"
}
```

## Configuration

### Application Properties
Enhanced health check configuration in `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,circuitbreakers
  endpoint:
    health:
      show-details: always
      show-components: always
      group:
        liveness:
          include: livenessState,diskSpace
        readiness:
          include: readinessState,db,circuitBreakers,transcriptionService,storage
      probes:
        enabled: true
  health:
    circuitbreakers:
      enabled: true
    defaults:
      enabled: true
    db:
      enabled: true
    diskspace:
      enabled: true
      threshold: 1GB
  metrics:
    tags:
      application: speech-to-text-api
      service: api-service
```

### Health Group Endpoints
- **Liveness Probe**: `/actuator/health/liveness` - Basic system liveness
- **Readiness Probe**: `/actuator/health/readiness` - Service readiness for traffic

## Monitoring Integration

### Prometheus Metrics
Health indicators automatically expose metrics for monitoring:

```yaml
# Health check response times
health_check_duration_seconds{component="database"}
health_check_duration_seconds{component="storage"}
health_check_duration_seconds{component="transcriptionService"}

# Health status indicators (1 = UP, 0.5 = DEGRADED, 0 = DOWN)
health_status{component="database"}
health_status{component="storage"}
health_status{component="business"}
```

### Grafana Dashboard Queries
Example Grafana queries for health monitoring:

```promql
# Overall system health
sum(health_status) / count(health_status)

# Component-specific health over time
health_status{component="database"}

# Health check response times
rate(health_check_duration_seconds_sum[5m]) / rate(health_check_duration_seconds_count[5m])
```

### Alerting Rules
Example Prometheus alerting rules:

```yaml
groups:
  - name: health_checks
    rules:
      - alert: ComponentDown
        expr: health_status == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Component {{ $labels.component }} is DOWN"
          
      - alert: ComponentDegraded
        expr: health_status == 0.5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Component {{ $labels.component }} is DEGRADED"
```

## Best Practices

### 1. Health Check Design
- **Fast Response Times**: Keep health checks under 5 seconds
- **Meaningful Tests**: Test actual functionality, not just connectivity
- **Graceful Degradation**: Distinguish between degraded and down states
- **Resource Cleanup**: Always clean up test resources (files, connections)

### 2. Monitoring Strategy
- **Regular Monitoring**: Check health endpoints every 30 seconds
- **Threshold Tuning**: Adjust thresholds based on actual system behavior
- **Alert Fatigue Prevention**: Use appropriate alert severity levels
- **Historical Analysis**: Track health trends over time

### 3. Operational Procedures
- **Health Dashboard**: Create operations dashboard showing all health statuses
- **Escalation Procedures**: Define clear escalation paths for different health states
- **Documentation**: Keep health check documentation up to date
- **Testing**: Regularly test health checks under various failure scenarios

## Troubleshooting

### Common Health Check Issues

#### Database Health Issues
```bash
# Check database connectivity
kubectl exec -it api-pod -- psql -h postgres -U user -d speechtotext -c "SELECT 1"

# Check connection pool status
kubectl logs api-pod | grep -i "connection pool"
```

#### Storage Health Issues
```bash
# Test S3/MinIO connectivity
kubectl exec -it api-pod -- curl -I http://minio:9000/minio/health/live

# Check bucket configuration
kubectl exec -it api-pod -- aws s3 ls s3://speechtotext-audio --endpoint-url=http://minio:9000
```

#### Circuit Breaker Issues
```bash
# Check circuit breaker state
curl http://api-service/internal/v1/circuit-breaker/transcription-service/status

# Reset circuit breaker (if needed)
curl -X POST http://api-service/internal/v1/circuit-breaker/transcription-service/reset
```

### Health Check Debugging
Enable debug logging for health checks:

```yaml
logging:
  level:
    com.speechtotext.api.health: DEBUG
    org.springframework.boot.actuator.health: DEBUG
```

## Integration with Deployment

### Kubernetes Probes
```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      containers:
      - name: api-service
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 5
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
```

### Load Balancer Health Checks
```nginx
upstream api_backend {
    server api-1:8080 max_fails=3 fail_timeout=30s;
    server api-2:8080 max_fails=3 fail_timeout=30s;
}

location /health {
    proxy_pass http://api_backend/internal/v1/health/quick;
    proxy_connect_timeout 2s;
    proxy_send_timeout 2s;
    proxy_read_timeout 2s;
}
```

The enhanced health check system provides comprehensive visibility into system status, enabling proactive monitoring and faster incident response for the Speech to Text service.

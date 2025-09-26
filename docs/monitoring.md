# Monitoring and Observability Guide

This document provides comprehensive guidelines for monitoring, logging, and observability of the Speech to Text service in production.

## 📋 Table of Contents

- [Monitoring Stack Overview](#monitoring-stack-overview)
- [Metrics Collection](#metrics-collection)
- [Logging Configuration](#logging-configuration)
- [Alerting Rules](#alerting-rules)
- [Dashboard Configuration](#dashboard-configuration)
- [Health Checks](#health-checks)
- [Performance Monitoring](#performance-monitoring)
- [Distributed Tracing](#distributed-tracing)

## 🏗️ Monitoring Stack Overview

### Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Application   │───▶│   Prometheus    │───▶│    Grafana      │
│   Metrics       │    │   (Metrics DB)  │    │  (Dashboards)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │                        │
┌─────────────────┐              │              ┌─────────────────┐
│   Log Files     │              │              │   Alertmanager  │
│   (JSON/Text)   │              │              │   (Alerts)      │
└─────────────────┘              │              └─────────────────┘
          │                      │                        │
          ▼                      ▼                        ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Loki/ELK      │    │   Jaeger        │    │   PagerDuty/    │
│   (Log Store)   │    │   (Tracing)     │    │   Slack         │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Components

- **Prometheus**: Metrics collection and storage
- **Grafana**: Dashboards and visualization
- **Alertmanager**: Alert routing and management
- **Loki**: Log aggregation (alternative: ELK stack)
- **Jaeger**: Distributed tracing
- **cAdvisor**: Container metrics

## 📊 Metrics Collection

### Prometheus Configuration

Create `prometheus.yml`:

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "alert_rules.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

scrape_configs:
  # API Service metrics
  - job_name: 'api-service'
    scrape_interval: 10s
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['api-service:8080']
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
        regex: '(.+)'
        replacement: '${1}'

  # Transcription Service metrics
  - job_name: 'transcription-service'
    scrape_interval: 15s
    metrics_path: '/metrics'
    static_configs:
      - targets: ['transcription-service:8081']

  # PostgreSQL metrics
  - job_name: 'postgres'
    scrape_interval: 30s
    static_configs:
      - targets: ['postgres-exporter:9187']

  # MinIO metrics
  - job_name: 'minio'
    scrape_interval: 30s
    metrics_path: '/minio/v2/metrics/cluster'
    static_configs:
      - targets: ['minio:9000']

  # Node metrics
  - job_name: 'node-exporter'
    scrape_interval: 30s
    static_configs:
      - targets: ['node-exporter:9100']

  # Container metrics
  - job_name: 'cadvisor'
    scrape_interval: 30s
    static_configs:
      - targets: ['cadvisor:8080']

  # HAProxy metrics
  - job_name: 'haproxy'
    scrape_interval: 30s
    static_configs:
      - targets: ['haproxy-exporter:9101']
```

### Custom Application Metrics

#### Java/Spring Boot Metrics

The API service exposes custom metrics via Micrometer:

```java
// Transcription metrics (already implemented)
- transcription.uploads.total
- transcription.success.total  
- transcription.failures.total
- transcription.duration
- transcription.sync.total
- transcription.async.total

// Additional business metrics
- transcription.file.size.bytes (histogram)
- transcription.processing.queue.size
- transcription.callback.success.total
- transcription.callback.failures.total
```

#### Python/FastAPI Metrics

Create `services/transcription-service/app/metrics.py`:

```python
from prometheus_client import Counter, Histogram, Gauge, generate_latest
import time

# Request metrics
REQUEST_COUNT = Counter(
    'transcription_requests_total',
    'Total transcription requests',
    ['method', 'endpoint', 'status']
)

REQUEST_DURATION = Histogram(
    'transcription_request_duration_seconds',
    'Time spent processing transcription requests',
    ['method', 'endpoint']
)

# Processing metrics
TRANSCRIPTION_DURATION = Histogram(
    'whisperx_processing_duration_seconds',
    'Time spent in WhisperX processing',
    ['model', 'language']
)

MODEL_LOAD_DURATION = Histogram(
    'whisperx_model_load_duration_seconds',
    'Time spent loading WhisperX models',
    ['model']
)

ACTIVE_TRANSCRIPTIONS = Gauge(
    'active_transcriptions_count',
    'Number of currently active transcriptions'
)

# Error metrics
TRANSCRIPTION_ERRORS = Counter(
    'transcription_errors_total',
    'Total transcription errors',
    ['error_type', 'model']
)

CALLBACK_ERRORS = Counter(
    'callback_errors_total',
    'Total callback errors',
    ['error_type']
)

# Resource metrics
GPU_MEMORY_USAGE = Gauge(
    'gpu_memory_usage_bytes',
    'GPU memory usage in bytes'
)

CPU_USAGE_PERCENT = Gauge(
    'cpu_usage_percent',
    'CPU usage percentage'
)
```

## 📝 Logging Configuration

### Structured Logging

#### API Service (Logback configuration)

Create `services/api-service/src/main/resources/logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProfile name="prod">
        <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp/>
                    <version/>
                    <logLevel/>
                    <message/>
                    <mdc/>
                    <arguments/>
                    <stackTrace/>
                </providers>
            </encoder>
        </appender>

        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/api-service.log</file>
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp/>
                    <version/>
                    <logLevel/>
                    <message/>
                    <mdc/>
                    <arguments/>
                    <stackTrace/>
                </providers>
            </encoder>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>logs/api-service.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
                <maxFileSize>100MB</maxFileSize>
                <maxHistory>30</maxHistory>
                <totalSizeCap>10GB</totalSizeCap>
            </rollingPolicy>
        </appender>

        <root level="INFO">
            <appender-ref ref="STDOUT"/>
            <appender-ref ref="FILE"/>
        </root>
        
        <logger name="com.speechtotext.api" level="DEBUG"/>
        <logger name="org.springframework.web" level="INFO"/>
    </springProfile>
</configuration>
```

#### Transcription Service (Python logging)

Update `services/transcription-service/app/main.py`:

```python
import logging
import sys
import json
from datetime import datetime

# Configure structured logging
class JSONFormatter(logging.Formatter):
    def format(self, record):
        log_entry = {
            'timestamp': datetime.utcnow().isoformat(),
            'level': record.levelname,
            'message': record.getMessage(),
            'module': record.module,
            'function': record.funcName,
            'line': record.lineno
        }
        
        if hasattr(record, 'job_id'):
            log_entry['job_id'] = record.job_id
        if hasattr(record, 'user_id'):
            log_entry['user_id'] = record.user_id
        if hasattr(record, 'duration'):
            log_entry['duration'] = record.duration
            
        if record.exc_info:
            log_entry['exception'] = self.formatException(record.exc_info)
            
        return json.dumps(log_entry)

# Setup logging
logger = logging.getLogger(__name__)
handler = logging.StreamHandler(sys.stdout)
handler.setFormatter(JSONFormatter())
logger.addHandler(handler)
logger.setLevel(logging.INFO)
```

### Log Aggregation with Loki

Create `loki-config.yml`:

```yaml
auth_enabled: false

server:
  http_listen_port: 3100

ingester:
  lifecycler:
    address: 127.0.0.1
    ring:
      kvstore:
        store: inmemory
      replication_factor: 1
    final_sleep: 0s
  chunk_idle_period: 1h
  max_chunk_age: 1h
  chunk_target_size: 1048576
  chunk_retain_period: 30s

schema_config:
  configs:
    - from: 2023-01-01
      store: boltdb-shipper
      object_store: filesystem
      schema: v11
      index:
        prefix: index_
        period: 24h

storage_config:
  boltdb_shipper:
    active_index_directory: /loki/boltdb-shipper-active
    cache_location: /loki/boltdb-shipper-cache
    shared_store: filesystem
  filesystem:
    directory: /loki/chunks

limits_config:
  reject_old_samples: true
  reject_old_samples_max_age: 168h

chunk_store_config:
  max_look_back_period: 0s

table_manager:
  retention_deletes_enabled: false
  retention_period: 0s
```

## 🚨 Alerting Rules

Create `alert_rules.yml`:

```yaml
groups:
  - name: speechtotext.rules
    rules:
      # Service availability alerts
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Service {{ $labels.job }} is down"
          description: "{{ $labels.job }} has been down for more than 1 minute"

      # API Service alerts
      - alert: HighErrorRate
        expr: rate(transcription_failures_total[5m]) / rate(transcription_uploads_total[5m]) > 0.1
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High error rate in transcription service"
          description: "Error rate is {{ $value | humanizePercentage }}"

      - alert: HighResponseTime
        expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High response time"
          description: "95th percentile response time is {{ $value }}s"

      # Database alerts
      - alert: DatabaseConnectionsHigh
        expr: pg_stat_database_numbackends / pg_settings_max_connections > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Database connection pool nearly exhausted"
          description: "Database connections are at {{ $value | humanizePercentage }}"

      - alert: DatabaseDiskSpaceLow
        expr: (node_filesystem_avail_bytes{mountpoint="/var/lib/postgresql/data"} / node_filesystem_size_bytes{mountpoint="/var/lib/postgresql/data"}) < 0.2
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Database disk space low"
          description: "Database disk space is {{ $value | humanizePercentage }} full"

      # Resource alerts
      - alert: HighCPUUsage
        expr: 100 - (avg by(instance) (rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High CPU usage"
          description: "CPU usage is {{ $value }}%"

      - alert: HighMemoryUsage
        expr: (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) > 0.85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage"
          description: "Memory usage is {{ $value | humanizePercentage }}"

      # Application-specific alerts
      - alert: LongRunningTranscriptions
        expr: histogram_quantile(0.95, rate(transcription_duration_bucket[10m])) > 300
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Transcriptions taking too long"
          description: "95th percentile transcription time is {{ $value }}s"

      - alert: TranscriptionQueueBacklog
        expr: transcription_processing_queue_size > 100
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "Large transcription queue backlog"
          description: "Queue size is {{ $value }}"

      # Storage alerts
      - alert: S3UploadFailures
        expr: rate(s3_upload_failures_total[5m]) > 0.1
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High S3 upload failure rate"
          description: "S3 upload failure rate is {{ $value }}/s"
```

### Alertmanager Configuration

Create `alertmanager.yml`:

```yaml
global:
  smtp_smarthost: 'localhost:587'
  smtp_from: 'alerts@yourdomain.com'

route:
  group_by: ['alertname']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'web.hook'
  routes:
  - match:
      severity: critical
    receiver: 'critical-alerts'
  - match:
      severity: warning
    receiver: 'warning-alerts'

receivers:
- name: 'web.hook'
  webhook_configs:
  - url: 'http://localhost:5001/'

- name: 'critical-alerts'
  pagerduty_configs:
  - service_key: 'your-pagerduty-service-key'
    description: 'Critical alert: {{ range .Alerts }}{{ .Annotations.summary }}{{ end }}'
  slack_configs:
  - api_url: 'your-slack-webhook-url'
    channel: '#alerts-critical'
    title: 'Critical Alert'
    text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'

- name: 'warning-alerts'
  slack_configs:
  - api_url: 'your-slack-webhook-url'
    channel: '#alerts-warning'
    title: 'Warning Alert'
    text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'
```

## 📊 Dashboard Configuration

### Grafana Dashboard for API Service

Create `grafana-dashboard-api.json`:

```json
{
  "dashboard": {
    "id": null,
    "title": "Speech to Text API Service",
    "tags": ["speechtotext", "api"],
    "timezone": "browser",
    "panels": [
      {
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_requests_total[5m])",
            "legendFormat": "{{method}} {{uri}}"
          }
        ],
        "yAxes": [
          {
            "label": "Requests/sec"
          }
        ]
      },
      {
        "title": "Response Time",
        "type": "graph", 
        "targets": [
          {
            "expr": "histogram_quantile(0.50, rate(http_request_duration_seconds_bucket[5m]))",
            "legendFormat": "50th percentile"
          },
          {
            "expr": "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))",
            "legendFormat": "95th percentile"
          },
          {
            "expr": "histogram_quantile(0.99, rate(http_request_duration_seconds_bucket[5m]))",
            "legendFormat": "99th percentile"
          }
        ],
        "yAxes": [
          {
            "label": "Seconds"
          }
        ]
      },
      {
        "title": "Error Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(transcription_failures_total[5m]) / rate(transcription_uploads_total[5m])",
            "legendFormat": "Error Rate"
          }
        ],
        "yAxes": [
          {
            "label": "Percentage",
            "max": 1
          }
        ]
      },
      {
        "title": "Transcription Processing Time",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(transcription_duration_bucket[5m]))",
            "legendFormat": "95th percentile"
          }
        ]
      }
    ]
  }
}
```

### System Resource Dashboard

```json
{
  "dashboard": {
    "title": "System Resources",
    "panels": [
      {
        "title": "CPU Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "100 - (avg by(instance) (rate(node_cpu_seconds_total{mode=\"idle\"}[5m])) * 100)",
            "legendFormat": "{{instance}}"
          }
        ]
      },
      {
        "title": "Memory Usage",
        "type": "graph", 
        "targets": [
          {
            "expr": "(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100",
            "legendFormat": "{{instance}}"
          }
        ]
      },
      {
        "title": "Disk Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "100 - (node_filesystem_avail_bytes / node_filesystem_size_bytes * 100)",
            "legendFormat": "{{instance}} {{mountpoint}}"
          }
        ]
      }
    ]
  }
}
```

## 🏥 Health Checks

### Comprehensive Health Check Implementation

#### API Service Health Check

```java
@Component
public class TranscriptionServiceHealthIndicator implements HealthIndicator {
    
    @Autowired
    private TranscriptionServiceClient transcriptionServiceClient;
    
    @Autowired
    private JobRepository jobRepository;
    
    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        
        // Check database connectivity
        try {
            jobRepository.count();
            builder.withDetail("database", "UP");
        } catch (Exception e) {
            builder.down().withDetail("database", "DOWN - " + e.getMessage());
        }
        
        // Check transcription service connectivity
        try {
            transcriptionServiceClient.healthCheck();
            builder.withDetail("transcription-service", "UP");
        } catch (Exception e) {
            builder.withDetail("transcription-service", "DOWN - " + e.getMessage());
        }
        
        // Check S3 connectivity
        try {
            // Perform a simple S3 operation
            builder.withDetail("s3", "UP");
        } catch (Exception e) {
            builder.withDetail("s3", "DOWN - " + e.getMessage());
        }
        
        return builder.build();
    }
}
```

#### Python Service Health Check

```python
@app.get("/health")
async def health_check():
    health_status = {
        "status": "healthy",
        "timestamp": datetime.utcnow().isoformat(),
        "checks": {}
    }
    
    # Check WhisperX model availability
    try:
        if transcription_service.is_ready():
            health_status["checks"]["whisperx"] = "UP"
        else:
            health_status["checks"]["whisperx"] = "DOWN - Models not loaded"
            health_status["status"] = "unhealthy"
    except Exception as e:
        health_status["checks"]["whisperx"] = f"DOWN - {str(e)}"
        health_status["status"] = "unhealthy"
    
    # Check S3 connectivity
    try:
        # Perform a simple S3 operation
        health_status["checks"]["s3"] = "UP"
    except Exception as e:
        health_status["checks"]["s3"] = f"DOWN - {str(e)}"
        health_status["status"] = "unhealthy"
    
    # Check GPU availability (if configured)
    try:
        if torch.cuda.is_available():
            health_status["checks"]["gpu"] = "UP"
            health_status["gpu_count"] = torch.cuda.device_count()
        else:
            health_status["checks"]["gpu"] = "N/A - CPU mode"
    except Exception as e:
        health_status["checks"]["gpu"] = f"ERROR - {str(e)}"
    
    status_code = 200 if health_status["status"] == "healthy" else 503
    return JSONResponse(content=health_status, status_code=status_code)
```

## 📈 Performance Monitoring

### Key Performance Indicators (KPIs)

1. **Throughput Metrics**
   - Requests per second
   - Transcriptions per minute
   - File upload rate

2. **Latency Metrics**
   - API response time (p50, p95, p99)
   - Transcription processing time
   - End-to-end processing time

3. **Error Metrics**
   - Error rate by endpoint
   - Failed transcription rate
   - Timeout rate

4. **Resource Utilization**
   - CPU usage
   - Memory usage
   - GPU utilization
   - Disk I/O
   - Network I/O

### Performance Benchmarking

Create performance test scripts to establish baselines:

```bash
#!/bin/bash
# Performance benchmark script

echo "Running performance benchmarks..."

# API endpoint load test
echo "Testing API throughput..."
ab -n 1000 -c 10 -H "Accept: application/json" \
   http://localhost:8080/api/v1/transcriptions/health

# File upload test
echo "Testing file upload performance..."
for i in {1..10}; do
    time curl -X POST \
        -F "file=@test-audio.wav" \
        -F "language=en" \
        -F "sync=false" \
        http://localhost:8080/api/v1/transcriptions
done
```

## 🔍 Distributed Tracing

### Jaeger Configuration

Add tracing to both services for request correlation:

#### API Service Tracing

```java
@RestController
public class TranscriptionController {
    
    @Autowired
    private Tracer tracer;
    
    @PostMapping("/transcriptions")
    public ResponseEntity<?> uploadFile(@RequestParam MultipartFile file) {
        Span span = tracer.nextSpan()
            .name("upload-transcription")
            .tag("file.size", String.valueOf(file.getSize()))
            .tag("file.type", file.getContentType())
            .start();
        
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            // Processing logic here
            return processUpload(file);
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

#### Python Service Tracing

```python
from opentelemetry import trace
from opentelemetry.exporter.jaeger.thrift import JaegerExporter
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor

# Configure tracing
trace.set_tracer_provider(TracerProvider())
tracer = trace.get_tracer(__name__)

jaeger_exporter = JaegerExporter(
    agent_host_name="jaeger",
    agent_port=6831,
)

span_processor = BatchSpanProcessor(jaeger_exporter)
trace.get_tracer_provider().add_span_processor(span_processor)

@app.post("/transcribe")
async def transcribe_audio(request: TranscriptionRequest):
    with tracer.start_as_current_span("transcribe-audio") as span:
        span.set_attribute("job.id", request.job_id)
        span.set_attribute("model", request.model)
        span.set_attribute("language", request.language)
        
        try:
            result = await process_transcription(request)
            span.set_attribute("transcription.duration", result.duration)
            return result
        except Exception as e:
            span.record_exception(e)
            span.set_status(trace.Status(trace.StatusCode.ERROR, str(e)))
            raise
```

## 🚀 Deployment

Deploy the monitoring stack using Docker Compose:

```yaml
# monitoring-stack.yml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - ./alert_rules.yml:/etc/prometheus/alert_rules.yml
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--web.enable-lifecycle'

  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3000:3000"
    volumes:
      - grafana-storage:/var/lib/grafana
      - ./grafana/provisioning:/etc/grafana/provisioning
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin

  alertmanager:
    image: prom/alertmanager:latest
    container_name: alertmanager
    ports:
      - "9093:9093"
    volumes:
      - ./alertmanager.yml:/etc/alertmanager/alertmanager.yml

volumes:
  grafana-storage:
```

This monitoring setup provides comprehensive observability for your Speech to Text service, enabling proactive monitoring, alerting, and performance optimization.

# Troubleshooting Guide

Comprehensive troubleshooting guide for the Speech to Text service covering common issues, diagnostic procedures, and resolution steps.

## 📋 Table of Contents

- [Quick Diagnostic Checklist](#quick-diagnostic-checklist)
- [Common Issues](#common-issues)
- [Service-Specific Troubleshooting](#service-specific-troubleshooting)
- [Performance Issues](#performance-issues)
- [Integration Issues](#integration-issues)
- [Infrastructure Issues](#infrastructure-issues)
- [Monitoring and Diagnostics](#monitoring-and-diagnostics)
- [Recovery Procedures](#recovery-procedures)
- [Support and Escalation](#support-and-escalation)

## 🚀 Quick Diagnostic Checklist

When encountering issues, run through this checklist first:

### 1. Service Health Check
```bash
# Check API service health
curl -X GET "https://api.yourdomain.com/api/v1/health" \
  -H "Authorization: Bearer YOUR_API_KEY"

# Expected response: HTTP 200 with status "healthy"
```

### 2. Authentication Check
```bash
# Verify API key is working
curl -X GET "https://api.yourdomain.com/api/v1/transcriptions" \
  -H "Authorization: Bearer YOUR_API_KEY"

# Expected response: HTTP 200 with transcription list (or empty list)
```

### 3. Service Dependencies
```bash
# Check database connectivity
curl -X GET "https://api.yourdomain.com/api/v1/health" | jq '.services.database'

# Check storage connectivity  
curl -X GET "https://api.yourdomain.com/api/v1/health" | jq '.services.storage'

# Check transcription service
curl -X GET "https://api.yourdomain.com/api/v1/health" | jq '.services.transcription_service'
```

### 4. Container Status (Docker/Kubernetes)
```bash
# Docker Compose
docker-compose ps

# Kubernetes
kubectl get pods -n speechtotext
kubectl get services -n speechtotext
```

## 🔧 Common Issues

### Issue 1: Upload Fails with "File Too Large" Error

**Symptoms:**
- HTTP 413 Payload Too Large
- Error: `FILE_TOO_LARGE`

**Diagnosis:**
```bash
# Check file size
ls -lh your-audio-file.wav

# Check current file size limits
curl -X GET "https://api.yourdomain.com/api/v1/health" | jq '.limits'
```

**Resolution:**
1. **Reduce file size:**
   ```bash
   # Using ffmpeg to compress audio
   ffmpeg -i large-audio.wav -ac 1 -ar 16000 -ab 64k compressed-audio.wav
   ```

2. **Increase server limits (Admin only):**
   ```yaml
   # docker-compose.yml
   api-service:
     environment:
       - SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE=500MB
       - SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE=500MB
   ```

3. **Split large files:**
   ```bash
   # Split audio file into chunks
   ffmpeg -i large-audio.wav -f segment -segment_time 300 -c copy chunk_%03d.wav
   ```

### Issue 2: Transcription Stuck in "PROCESSING" Status

**Symptoms:**
- Job status remains "PROCESSING" for extended periods
- No progress updates
- Queue appears stalled

**Diagnosis:**
```bash
# Check transcription service logs
docker logs speechtotext-transcription-service-1

# Check queue status
curl -X GET "https://api.yourdomain.com/api/v1/health" | jq '.metrics.queue_size'

# Check active transcriptions
curl -X GET "https://api.yourdomain.com/api/v1/health" | jq '.metrics.active_transcriptions'
```

**Resolution:**
1. **Restart transcription service:**
   ```bash
   # Docker Compose
   docker-compose restart transcription-service
   
   # Kubernetes
   kubectl delete pod -l app=transcription-service -n speechtotext
   ```

2. **Check resource availability:**
   ```bash
   # Check memory usage
   docker stats
   
   # Check GPU availability (if using GPU)
   nvidia-smi
   ```

3. **Clear stuck jobs:**
   ```bash
   # Connect to database and reset stuck jobs
   docker exec -it postgres-container psql -U speechtotext -d speechtotext
   
   UPDATE transcription_jobs 
   SET status = 'FAILED', 
        error_message = 'Job timeout - resubmit if needed',
        updated_at = NOW()
   WHERE status = 'PROCESSING' 
     AND updated_at < NOW() - INTERVAL '30 minutes';
   ```

### Issue 3: Database Connection Issues

**Symptoms:**
- API returns 500 Internal Server Error
- Health check shows database as "unhealthy"
- Connection timeout errors

**Diagnosis:**
```bash
# Check database container status
docker ps | grep postgres

# Test database connectivity
docker exec -it postgres-container pg_isready -U speechtotext

# Check database logs
docker logs postgres-container
```

**Resolution:**
1. **Restart database:**
   ```bash
   docker-compose restart postgres
   ```

2. **Check connection configuration:**
   ```yaml
   # Verify connection settings in application.yml
   spring:
     datasource:
       url: jdbc:postgresql://postgres:5432/speechtotext
       username: speechtotext
       password: ${DB_PASSWORD}
   ```

3. **Database recovery:**
   ```bash
   # If database is corrupted
   docker-compose down
   docker volume rm speechtotext_postgres_data
   docker-compose up -d postgres
   
   # Wait for database initialization, then restart services
   docker-compose up -d
   ```

### Issue 4: Storage Service Unavailable

**Symptoms:**
- Cannot upload files
- Download URLs return 404
- Storage health check fails

**Diagnosis:**
```bash
# Check MinIO/S3 container
docker ps | grep minio

# Test storage connectivity
curl -X GET "http://localhost:9000/minio/health/live"

# Check storage logs
docker logs minio-container
```

**Resolution:**
1. **Restart storage service:**
   ```bash
   docker-compose restart minio
   ```

2. **Verify storage configuration:**
   ```yaml
   # Check bucket configuration
   AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID}
   AWS_SECRET_ACCESS_KEY: ${AWS_SECRET_ACCESS_KEY}
   AWS_REGION: ${AWS_REGION}
   S3_BUCKET_NAME: ${S3_BUCKET_NAME}
   ```

3. **Recreate storage bucket:**
   ```bash
   # Access MinIO console
   mc alias set myminio http://localhost:9000 minioadmin minioadmin
   mc mb myminio/speechtotext-audio
   mc mb myminio/speechtotext-transcripts
   ```

### Issue 5: Out of Memory Errors

**Symptoms:**
- Services crash with OOM errors
- Performance degradation
- Failed transcription jobs

**Diagnosis:**
```bash
# Check memory usage
docker stats

# Check Java heap usage
docker exec -it api-service-container jstat -gc 1

# Check system memory
free -h
```

**Resolution:**
1. **Increase container memory limits:**
   ```yaml
   # docker-compose.yml
   api-service:
     mem_limit: 2g
     
   transcription-service:
     mem_limit: 4g
   ```

2. **Optimize Java heap settings:**
   ```yaml
   api-service:
     environment:
       - JAVA_OPTS=-Xms512m -Xmx1536m -XX:+UseG1GC
   ```

3. **Scale horizontally:**
   ```bash
   # Scale transcription service
   docker-compose up -d --scale transcription-service=3
   ```

## 🎯 Service-Specific Troubleshooting

### API Service (Spring Boot)

#### Application Won't Start
```bash
# Check logs
docker logs api-service-container

# Common issues to check:
# 1. Database connection
# 2. Missing environment variables
# 3. Port conflicts
# 4. Configuration errors
```

**Common Solutions:**
1. **Database connectivity:**
   ```bash
   # Wait for database to be ready
   docker-compose up -d postgres
   sleep 30
   docker-compose up -d api-service
   ```

2. **Environment variables:**
   ```bash
   # Check required environment variables
   docker exec api-service-container env | grep -E "(DB_|AWS_|JWT_)"
   ```

3. **Port conflicts:**
   ```bash
   # Check if port 8080 is in use
   lsof -i :8080
   
   # Change port if needed
   export API_PORT=8081
   docker-compose up -d
   ```

#### Slow API Response Times
```bash
# Check thread pool usage
curl -X GET "http://localhost:8080/actuator/metrics/http.server.requests"

# Check database query performance
curl -X GET "http://localhost:8080/actuator/metrics/hikari.connections.active"
```

**Solutions:**
1. **Database optimization:**
   ```sql
   -- Add missing indexes
   CREATE INDEX idx_transcription_status ON transcription_jobs(status);
   CREATE INDEX idx_transcription_created_at ON transcription_jobs(created_at);
   ```

2. **Connection pool tuning:**
   ```yaml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 20
         minimum-idle: 5
   ```

### Transcription Service (Python/FastAPI)

#### WhisperX Model Loading Issues
```bash
# Check model loading logs
docker logs transcription-service-container | grep -i "model"

# Check available disk space
df -h

# Check GPU availability
nvidia-smi
```

**Solutions:**
1. **Clear model cache:**
   ```bash
   docker exec -it transcription-service-container rm -rf /root/.cache/whisper*
   docker-compose restart transcription-service
   ```

2. **Preload models:**
   ```python
   # Add to startup script
   import whisperx
   
   models = ["tiny", "base", "small"]
   for model_name in models:
       whisperx.load_model(model_name, device="cuda")
   ```

#### Python Process Crashes
```bash
# Check for segmentation faults
docker logs transcription-service-container | grep -i "segmentation"

# Check memory usage
docker exec transcription-service-container cat /proc/meminfo
```

**Solutions:**
1. **Update dependencies:**
   ```bash
   # Rebuild with updated requirements
   docker-compose build transcription-service
   ```

2. **Add memory monitoring:**
   ```python
   import psutil
   
   def check_memory():
       memory = psutil.virtual_memory()
       if memory.percent > 90:
           logging.warning(f"High memory usage: {memory.percent}%")
   ```

## ⚡ Performance Issues

### High CPU Usage

**Diagnosis:**
```bash
# Check CPU usage by container
docker stats --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}"

# Check system load
uptime
top
```

**Solutions:**
1. **Scale services:**
   ```bash
   docker-compose up -d --scale transcription-service=3
   ```

2. **Optimize transcription batch size:**
   ```python
   # Reduce batch size for CPU-bound processing
   BATCH_SIZE = 8  # Reduce from default 16
   ```

3. **Use appropriate Whisper model:**
   ```python
   # Use smaller model for faster processing
   MODEL_SIZE = "base"  # Instead of "large"
   ```

### Memory Leaks

**Diagnosis:**
```bash
# Monitor memory usage over time
while true; do
  docker stats --no-stream --format "{{.Container}}: {{.MemUsage}}"
  sleep 60
done
```

**Solutions:**
1. **Implement memory cleanup:**
   ```python
   import gc
   
   def cleanup_memory():
       gc.collect()
       torch.cuda.empty_cache()  # If using GPU
   ```

2. **Add memory limits:**
   ```yaml
   transcription-service:
     deploy:
       resources:
         limits:
           memory: 4G
         reservations:
           memory: 2G
   ```

### Network Latency Issues

**Diagnosis:**
```bash
# Test network connectivity
ping api.yourdomain.com

# Check DNS resolution
nslookup api.yourdomain.com

# Test API latency
curl -w "@curl-format.txt" -X GET "https://api.yourdomain.com/api/v1/health"
```

**Solutions:**
1. **Enable connection pooling:**
   ```python
   # Use session for HTTP requests
   import requests
   
   session = requests.Session()
   session.mount('http://', requests.adapters.HTTPAdapter(pool_connections=100))
   ```

2. **Add CDN/caching:**
   ```nginx
   # Add to nginx configuration
   location /api/v1/transcriptions/ {
       proxy_cache transcription_cache;
       proxy_cache_valid 200 5m;
   }
   ```

## 🔌 Integration Issues

### API Authentication Problems

**Symptoms:**
- 401 Unauthorized responses
- Invalid API key errors
- Token expiration issues

**Diagnosis:**
```bash
# Test API key format
echo $API_KEY | base64 -d

# Check token expiration
jwt-decode $JWT_TOKEN
```

**Solutions:**
1. **Regenerate API key:**
   ```bash
   # Using admin endpoint
   curl -X POST "https://api.yourdomain.com/admin/api-keys" \
     -H "Authorization: Bearer ADMIN_TOKEN"
   ```

2. **Verify token format:**
   ```bash
   # JWT should have valid header, payload, signature
   echo $JWT_TOKEN | cut -d'.' -f1 | base64 -d
   ```

### Webhook Delivery Failures

**Diagnosis:**
```bash
# Check webhook logs
docker logs api-service-container | grep -i webhook

# Test webhook endpoint
curl -X POST "https://your-app.com/webhooks/transcription-complete" \
  -H "Content-Type: application/json" \
  -d '{"test": "data"}'
```

**Solutions:**
1. **Verify webhook URL:**
   ```bash
   # Test reachability
   curl -I https://your-app.com/webhooks/transcription-complete
   ```

2. **Check SSL certificates:**
   ```bash
   openssl s_client -connect your-app.com:443 -servername your-app.com
   ```

3. **Implement retry logic:**
   ```java
   @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))
   public void sendWebhook(String url, Object payload) {
       // Webhook sending logic
   }
   ```

## 🏗️ Infrastructure Issues

### Load Balancer Issues

**Diagnosis:**
```bash
# Check HAProxy status
curl -X GET "http://localhost:8404/stats"

# Test load balancing
for i in {1..10}; do
  curl -H "X-Request-ID: test-$i" https://api.yourdomain.com/api/v1/health
done
```

**Solutions:**
1. **Restart load balancer:**
   ```bash
   docker-compose restart haproxy
   ```

2. **Check backend health:**
   ```bash
   # Verify all backend services are healthy
   curl -X GET "http://api-service-1:8080/actuator/health"
   curl -X GET "http://api-service-2:8080/actuator/health"
   ```

### SSL/TLS Certificate Issues

**Diagnosis:**
```bash
# Check certificate expiration
openssl x509 -in /etc/ssl/certs/yourdomain.crt -text -noout | grep -A2 "Validity"

# Test SSL connection
openssl s_client -connect api.yourdomain.com:443
```

**Solutions:**
1. **Renew certificates:**
   ```bash
   # Using certbot
   certbot renew
   
   # Reload nginx/haproxy
   docker-compose exec haproxy kill -USR2 1
   ```

2. **Update certificate configuration:**
   ```yaml
   # docker-compose.yml
   haproxy:
     volumes:
       - ./certs:/etc/ssl/certs:ro
   ```

### Database Performance Issues

**Diagnosis:**
```bash
# Check slow queries
docker exec -it postgres-container psql -U speechtotext -c "
SELECT query, mean_time, calls 
FROM pg_stat_statements 
ORDER BY mean_time DESC LIMIT 10;"

# Check connection counts
docker exec -it postgres-container psql -U speechtotext -c "
SELECT count(*) FROM pg_stat_activity;"
```

**Solutions:**
1. **Add database indexes:**
   ```sql
   CREATE INDEX CONCURRENTLY idx_jobs_status_created 
   ON transcription_jobs(status, created_at);
   ```

2. **Optimize queries:**
   ```sql
   -- Use LIMIT for large result sets
   SELECT * FROM transcription_jobs 
   WHERE status = 'COMPLETED' 
   ORDER BY created_at DESC 
   LIMIT 100;
   ```

3. **Database maintenance:**
   ```bash
   # Run VACUUM and ANALYZE
   docker exec -it postgres-container psql -U speechtotext -c "VACUUM ANALYZE;"
   ```

## 📊 Monitoring and Diagnostics

### Health Check Scripts

```bash
#!/bin/bash
# health-check.sh

echo "=== Speech to Text Service Health Check ==="

# Check API service
echo "Checking API service..."
API_STATUS=$(curl -s -o /dev/null -w "%{http_code}" https://api.yourdomain.com/api/v1/health)
if [ "$API_STATUS" = "200" ]; then
    echo "✅ API service: healthy"
else
    echo "❌ API service: unhealthy (HTTP $API_STATUS)"
fi

# Check database
echo "Checking database..."
DB_STATUS=$(docker exec postgres-container pg_isready -U speechtotext >/dev/null 2>&1 && echo "ready" || echo "not ready")
if [ "$DB_STATUS" = "ready" ]; then
    echo "✅ Database: healthy"
else
    echo "❌ Database: unhealthy"
fi

# Check storage
echo "Checking storage..."
STORAGE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:9000/minio/health/live)
if [ "$STORAGE_STATUS" = "200" ]; then
    echo "✅ Storage: healthy"
else
    echo "❌ Storage: unhealthy (HTTP $STORAGE_STATUS)"
fi

# Check transcription service
echo "Checking transcription service..."
PYTHON_STATUS=$(docker exec transcription-service-container python -c "import sys; print('ok')" 2>/dev/null)
if [ "$PYTHON_STATUS" = "ok" ]; then
    echo "✅ Transcription service: healthy"
else
    echo "❌ Transcription service: unhealthy"
fi

echo "=== Health check completed ==="
```

### Log Analysis Scripts

```bash
#!/bin/bash
# log-analyzer.sh

echo "=== Log Analysis ==="

# Check for errors in the last hour
echo "Recent errors:"
docker logs api-service-container --since="1h" | grep -i error | tail -10

echo "Recent warnings:"
docker logs transcription-service-container --since="1h" | grep -i warning | tail -10

# Check memory usage patterns
echo "Memory usage trend:"
docker stats --no-stream --format "{{.Container}}: {{.MemUsage}}" | grep -E "(api|transcription)"

# Check disk usage
echo "Disk usage:"
df -h | grep -E "(var|tmp)"

echo "=== Log analysis completed ==="
```

### Performance Monitoring

```python
#!/usr/bin/env python3
# performance-monitor.py

import time
import requests
import psutil
import json
from datetime import datetime

def monitor_performance():
    metrics = {
        'timestamp': datetime.utcnow().isoformat(),
        'cpu_percent': psutil.cpu_percent(),
        'memory_percent': psutil.virtual_memory().percent,
        'disk_usage': psutil.disk_usage('/').percent,
        'api_response_time': None,
        'active_transcriptions': None
    }
    
    try:
        # Test API response time
        start_time = time.time()
        response = requests.get('https://api.yourdomain.com/api/v1/health', timeout=10)
        metrics['api_response_time'] = time.time() - start_time
        
        if response.status_code == 200:
            health_data = response.json()
            metrics['active_transcriptions'] = health_data.get('metrics', {}).get('active_transcriptions', 0)
    
    except Exception as e:
        metrics['error'] = str(e)
    
    print(json.dumps(metrics, indent=2))

if __name__ == '__main__':
    monitor_performance()
```

## 🚨 Recovery Procedures

### Complete Service Recovery

```bash
#!/bin/bash
# disaster-recovery.sh

echo "=== Starting Disaster Recovery ==="

# Stop all services
echo "Stopping all services..."
docker-compose down

# Backup current state
echo "Creating backup..."
mkdir -p backups/$(date +%Y%m%d_%H%M%S)
docker run --rm -v speechtotext_postgres_data:/volume -v $(pwd)/backups:/backup alpine sh -c "cd /volume && tar czf /backup/postgres_$(date +%Y%m%d_%H%M%S).tar.gz ."

# Clean up containers and volumes if needed
echo "Cleaning up (if required)..."
# docker system prune -f
# docker volume prune -f

# Restore from backup (if needed)
# echo "Restoring from backup..."
# docker run --rm -v speechtotext_postgres_data:/volume -v $(pwd)/backups:/backup alpine sh -c "cd /volume && tar xzf /backup/postgres_backup.tar.gz"

# Start infrastructure services first
echo "Starting infrastructure services..."
docker-compose up -d postgres minio

# Wait for services to be ready
echo "Waiting for infrastructure to be ready..."
sleep 30

# Start application services
echo "Starting application services..."
docker-compose up -d

# Verify recovery
echo "Verifying recovery..."
sleep 30
./health-check.sh

echo "=== Recovery completed ==="
```

### Database Recovery

```bash
#!/bin/bash
# db-recovery.sh

echo "=== Database Recovery ==="

# Create backup before recovery
pg_dump -h localhost -p 5432 -U speechtotext speechtotext > backup_before_recovery.sql

# Common recovery operations
echo "Choose recovery option:"
echo "1) Reset stuck transcription jobs"
echo "2) Clean up old completed jobs"
echo "3) Rebuild indexes"
echo "4) Full database reset"

read -p "Enter option (1-4): " option

case $option in
    1)
        echo "Resetting stuck jobs..."
        psql -h localhost -p 5432 -U speechtotext speechtotext -c "
        UPDATE transcription_jobs 
        SET status = 'FAILED', 
            error_message = 'Reset due to stuck status',
            updated_at = NOW()
        WHERE status IN ('PROCESSING', 'PENDING') 
          AND updated_at < NOW() - INTERVAL '1 hour';"
        ;;
    2)
        echo "Cleaning up old jobs..."
        psql -h localhost -p 5432 -U speechtotext speechtotext -c "
        DELETE FROM transcription_jobs 
        WHERE status = 'COMPLETED' 
          AND created_at < NOW() - INTERVAL '30 days';"
        ;;
    3)
        echo "Rebuilding indexes..."
        psql -h localhost -p 5432 -U speechtotext speechtotext -c "
        REINDEX DATABASE speechtotext;"
        ;;
    4)
        echo "Full database reset..."
        docker-compose down
        docker volume rm speechtotext_postgres_data
        docker-compose up -d postgres
        sleep 30
        docker-compose up -d
        ;;
esac

echo "=== Database recovery completed ==="
```

## 📞 Support and Escalation

### Log Collection for Support

```bash
#!/bin/bash
# collect-logs.sh

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_DIR="support_logs_$TIMESTAMP"

echo "Collecting logs for support..."
mkdir -p $LOG_DIR

# Collect service logs
docker logs api-service-container > $LOG_DIR/api-service.log 2>&1
docker logs transcription-service-container > $LOG_DIR/transcription-service.log 2>&1
docker logs postgres-container > $LOG_DIR/postgres.log 2>&1
docker logs minio-container > $LOG_DIR/minio.log 2>&1

# Collect system information
docker ps > $LOG_DIR/containers.txt
docker images > $LOG_DIR/images.txt
df -h > $LOG_DIR/disk_usage.txt
free -h > $LOG_DIR/memory_usage.txt
docker stats --no-stream > $LOG_DIR/container_stats.txt

# Collect configuration
docker-compose config > $LOG_DIR/docker-compose.yml
env | grep -E "(DB_|AWS_|API_)" > $LOG_DIR/environment.txt

# Collect recent API responses
curl -X GET "https://api.yourdomain.com/api/v1/health" > $LOG_DIR/health_check.json 2>&1

# Create archive
tar -czf "support_logs_$TIMESTAMP.tar.gz" $LOG_DIR/
rm -rf $LOG_DIR

echo "Logs collected in: support_logs_$TIMESTAMP.tar.gz"
echo "Please attach this file when contacting support."
```

### Escalation Matrix

| Severity | Response Time | Escalation Path | Contact Method |
|----------|---------------|-----------------|----------------|
| **Critical** (Service Down) | 15 minutes | DevOps → Engineering Lead → CTO | Phone + Slack |
| **High** (Performance Issues) | 1 hour | DevOps → Engineering Lead | Slack + Email |
| **Medium** (Feature Issues) | 4 hours | Support → Engineering | Email + Ticket |
| **Low** (Questions) | 24 hours | Support → Documentation | Email |

### Support Contact Information

```bash
# Emergency Contacts
DEVOPS_PHONE="+1-555-0123"
ONCALL_SLACK="#oncall-alerts"
SUPPORT_EMAIL="support@yourdomain.com"

# Issue Tracking
JIRA_PROJECT="SPEECHTOTEXT"
GITHUB_ISSUES="https://github.com/yourorg/speechtotext/issues"

# Documentation
RUNBOOK_URL="https://docs.yourdomain.com/runbook"
API_DOCS="https://docs.yourdomain.com/api"
```

---

This comprehensive troubleshooting guide should help resolve most common issues encountered with the Speech to Text service. Keep this document updated as new issues are discovered and resolved.

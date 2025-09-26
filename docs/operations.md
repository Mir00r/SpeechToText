# Operations Manual

This manual provides detailed procedures for operating, maintaining, and troubleshooting the Speech to Text service in production.

## 📋 Table of Contents

- [Daily Operations](#daily-operations)
- [Maintenance Procedures](#maintenance-procedures)
- [Incident Response](#incident-response)
- [Backup and Recovery](#backup-and-recovery)
- [Scaling Operations](#scaling-operations)
- [Security Operations](#security-operations)
- [Performance Tuning](#performance-tuning)
- [Troubleshooting](#troubleshooting)

## 🌅 Daily Operations

### Morning Health Check Routine

Execute this checklist every morning:

```bash
#!/bin/bash
# daily-health-check.sh

echo "🌅 Daily Health Check - $(date)"
echo "================================"

# 1. Check service availability
echo "1. Service Health Checks:"
curl -s http://api.yourdomain.com/actuator/health | jq '.'
curl -s http://transcription.yourdomain.com/health | jq '.'

# 2. Check key metrics
echo -e "\n2. Key Metrics (Last 24 hours):"
echo "   - Total requests: $(curl -s 'http://prometheus:9090/api/v1/query?query=increase(http_requests_total[24h])' | jq -r '.data.result[0].value[1]')"
echo "   - Error rate: $(curl -s 'http://prometheus:9090/api/v1/query?query=rate(transcription_failures_total[24h])/rate(transcription_uploads_total[24h])' | jq -r '.data.result[0].value[1]')"
echo "   - Avg response time: $(curl -s 'http://prometheus:9090/api/v1/query?query=rate(http_request_duration_seconds_sum[24h])/rate(http_request_duration_seconds_count[24h])' | jq -r '.data.result[0].value[1]')"

# 3. Check resource usage
echo -e "\n3. Resource Usage:"
echo "   - CPU: $(curl -s 'http://prometheus:9090/api/v1/query?query=100-(avg(rate(node_cpu_seconds_total{mode="idle"}[5m]))*100)' | jq -r '.data.result[0].value[1]')%"
echo "   - Memory: $(curl -s 'http://prometheus:9090/api/v1/query?query=(1-(node_memory_MemAvailable_bytes/node_memory_MemTotal_bytes))*100' | jq -r '.data.result[0].value[1]')%"
echo "   - Disk: $(df -h /var/lib/postgresql/data | awk 'NR==2{print $5}')"

# 4. Check active alerts
echo -e "\n4. Active Alerts:"
curl -s http://alertmanager:9093/api/v1/alerts | jq -r '.data[] | select(.status.state=="active") | "\(.labels.alertname): \(.annotations.summary)"'

# 5. Check backup status
echo -e "\n5. Backup Status:"
if [ -f "/backups/latest.success" ]; then
    echo "   ✅ Last backup: $(cat /backups/latest.success)"
else
    echo "   ❌ No recent backup found!"
fi

# 6. Check certificate expiration
echo -e "\n6. SSL Certificate Status:"
echo | openssl s_client -servername api.yourdomain.com -connect api.yourdomain.com:443 2>/dev/null | openssl x509 -noout -enddate

echo -e "\n✅ Daily health check completed"
```

### Key Metrics to Monitor Daily

| Metric | Threshold | Action |
|--------|-----------|---------|
| Error Rate | > 5% | Investigate logs |
| Response Time (95th) | > 2s | Check performance |
| CPU Usage | > 80% | Consider scaling |
| Memory Usage | > 85% | Check for leaks |
| Disk Usage | > 80% | Clean up or expand |
| Active Connections | > 80% of max | Monitor load |

## 🔧 Maintenance Procedures

### Weekly Maintenance Tasks

#### 1. System Updates
```bash
#!/bin/bash
# weekly-updates.sh

# Update system packages
sudo apt update && sudo apt upgrade -y

# Update Docker images
docker-compose pull
docker-compose up -d

# Clean up unused Docker resources
docker system prune -f
docker volume prune -f
```

#### 2. Log Rotation and Cleanup
```bash
#!/bin/bash
# log-cleanup.sh

# Rotate application logs
logrotate -f /etc/logrotate.d/speechtotext

# Clean old log files (older than 30 days)
find /var/log/speechtotext -name "*.log*" -type f -mtime +30 -delete

# Clean Docker logs
docker system prune --volumes -f
```

#### 3. Database Maintenance
```bash
#!/bin/bash
# db-maintenance.sh

# Connect to database and run maintenance
psql -h postgres-host -U speechtotext -d speechtotext << EOF

-- Update table statistics
ANALYZE;

-- Vacuum to reclaim space
VACUUM ANALYZE jobs;

-- Check index usage
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan ASC;

-- Check table sizes
SELECT schemaname, tablename, 
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

EOF
```

### Monthly Maintenance Tasks

#### 1. Security Updates
```bash
#!/bin/bash
# monthly-security.sh

# Update base images with security patches
docker pull postgres:15
docker pull minio/minio:latest

# Scan for vulnerabilities
trivy image ghcr.io/mir00r/speechtotext/api-service:latest
trivy image ghcr.io/mir00r/speechtotext/transcription-service:latest

# Update SSL certificates if needed
certbot renew --dry-run
```

#### 2. Performance Review
```bash
#!/bin/bash
# performance-review.sh

# Generate performance report
echo "📊 Monthly Performance Report - $(date)" > performance-report.txt
echo "==============================================" >> performance-report.txt

# Get metrics for the last 30 days
curl -s 'http://prometheus:9090/api/v1/query?query=avg_over_time(rate(http_requests_total[30d])[30d:1d])' \
    | jq -r '.data.result[0].value[1]' >> performance-report.txt

# Add capacity planning recommendations
echo "Capacity Planning:" >> performance-report.txt
echo "- Current average load: $(curl -s 'http://prometheus:9090/api/v1/query?query=rate(http_requests_total[30d])' | jq -r '.data.result[0].value[1]') req/s" >> performance-report.txt
echo "- Peak load: $(curl -s 'http://prometheus:9090/api/v1/query?query=max_over_time(rate(http_requests_total[5m])[30d])' | jq -r '.data.result[0].value[1]') req/s" >> performance-report.txt
```

## 🚨 Incident Response

### Incident Classification

| Severity | Description | Response Time | Example |
|----------|-------------|---------------|---------|
| P0 - Critical | Service completely down | 15 minutes | All API endpoints returning 5xx |
| P1 - High | Major functionality affected | 1 hour | Transcription service down |
| P2 - Medium | Minor functionality affected | 4 hours | Slow response times |
| P3 - Low | Cosmetic or future impact | 24 hours | Monitoring alert false positive |

### Incident Response Procedures

#### P0 - Critical Incident
```bash
#!/bin/bash
# critical-incident-response.sh

echo "🚨 CRITICAL INCIDENT RESPONSE INITIATED"
echo "Time: $(date)"

# 1. Immediate assessment
echo "1. Service Status Check:"
curl -I http://api.yourdomain.com/actuator/health
curl -I http://transcription.yourdomain.com/health

# 2. Check infrastructure
echo -e "\n2. Infrastructure Check:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# 3. Check recent changes
echo -e "\n3. Recent Deployments:"
kubectl get deployment -n speechtotext -o wide

# 4. Emergency rollback if needed
echo -e "\n4. Emergency Actions:"
echo "To rollback: kubectl rollout undo deployment/api-service -n speechtotext"
echo "To scale up: kubectl scale deployment/api-service --replicas=5 -n speechtotext"

# 5. Check logs for errors
echo -e "\n5. Recent Error Logs:"
kubectl logs -n speechtotext deployment/api-service --tail=100 | grep ERROR

# 6. Check resource exhaustion
echo -e "\n6. Resource Check:"
kubectl top nodes
kubectl top pods -n speechtotext
```

#### Communication Template

**Incident Notification:**
```
🚨 INCIDENT ALERT - P0

Service: Speech to Text API
Status: DOWN
Impact: All users affected
Start Time: [timestamp]
Incident Commander: [name]

Current Actions:
- Investigating root cause
- Implementing emergency measures
- ETA for resolution: [time]

Updates will be provided every 15 minutes.
```

### Post-Incident Review

After resolving any P0 or P1 incident, conduct a post-incident review:

1. **Timeline Documentation**
   - When was the incident first detected?
   - When was it resolved?
   - What was the impact?

2. **Root Cause Analysis**
   - What was the underlying cause?
   - What triggered the incident?
   - Were there warning signs?

3. **Response Evaluation**
   - Was the response time adequate?
   - Were the right people notified?
   - Were the tools and procedures effective?

4. **Action Items**
   - What can be improved?
   - What monitoring should be added?
   - What processes need updating?

## 💾 Backup and Recovery

### Automated Backup Procedures

#### Database Backup
```bash
#!/bin/bash
# database-backup.sh

BACKUP_DIR="/backups/postgresql"
RETENTION_DAYS=30
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="speechtotext_backup_${TIMESTAMP}.sql"

# Create backup directory if it doesn't exist
mkdir -p ${BACKUP_DIR}

# Perform backup
pg_dump -h postgres-host -U speechtotext -d speechtotext \
    --verbose --clean --no-owner --no-privileges \
    > "${BACKUP_DIR}/${BACKUP_FILE}"

# Compress backup
gzip "${BACKUP_DIR}/${BACKUP_FILE}"

# Verify backup integrity
if [ $? -eq 0 ]; then
    echo "✅ Database backup completed successfully: ${BACKUP_FILE}.gz"
    echo "${TIMESTAMP}" > /backups/latest.success
else
    echo "❌ Database backup failed!"
    exit 1
fi

# Clean old backups
find ${BACKUP_DIR} -name "*.gz" -type f -mtime +${RETENTION_DAYS} -delete
echo "🧹 Cleaned backups older than ${RETENTION_DAYS} days"

# Upload to S3 for off-site storage
aws s3 cp "${BACKUP_DIR}/${BACKUP_FILE}.gz" "s3://backups-bucket/postgresql/" \
    && echo "☁️ Backup uploaded to S3"
```

#### Object Storage Backup
```bash
#!/bin/bash
# s3-backup.sh

# Sync to backup bucket
aws s3 sync s3://speechtotext-prod s3://speechtotext-backup \
    --delete --exclude "*.tmp" \
    && echo "✅ S3 backup sync completed"

# Create manifest of all objects
aws s3 ls s3://speechtotext-prod --recursive > s3-manifest-$(date +%Y%m%d).txt
```

### Recovery Procedures

#### Database Recovery
```bash
#!/bin/bash
# database-recovery.sh

BACKUP_FILE="$1"

if [ -z "$BACKUP_FILE" ]; then
    echo "Usage: $0 <backup-file.gz>"
    exit 1
fi

echo "🔄 Starting database recovery from ${BACKUP_FILE}"

# Stop application services
docker-compose stop api-service transcription-service

# Restore database
gunzip -c "$BACKUP_FILE" | psql -h postgres-host -U speechtotext -d speechtotext

if [ $? -eq 0 ]; then
    echo "✅ Database recovery completed"
    
    # Restart services
    docker-compose start api-service transcription-service
    
    # Verify recovery
    sleep 30
    curl -f http://api.yourdomain.com/actuator/health
else
    echo "❌ Database recovery failed!"
    exit 1
fi
```

#### Disaster Recovery Plan

**Complete System Recovery:**

1. **Assessment Phase (0-15 minutes)**
   - Determine scope of disaster
   - Identify what needs recovery
   - Estimate recovery time

2. **Infrastructure Recovery (15-60 minutes)**
   - Provision new infrastructure if needed
   - Deploy monitoring and logging
   - Restore network configuration

3. **Data Recovery (30-120 minutes)**
   - Restore database from latest backup
   - Restore object storage data
   - Verify data integrity

4. **Application Recovery (60-180 minutes)**
   - Deploy application services
   - Update DNS if needed
   - Run smoke tests

5. **Verification Phase (180-240 minutes)**
   - Full system testing
   - Monitor for anomalies
   - Update stakeholders

**Recovery Time Objectives (RTO):**
- Database: 2 hours
- Application: 1 hour
- Complete system: 4 hours

**Recovery Point Objectives (RPO):**
- Database: 24 hours (daily backups)
- Object storage: 1 hour (continuous replication)

## ⚖️ Scaling Operations

### Horizontal Scaling

#### Auto-scaling with Kubernetes
```yaml
# hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: api-service-hpa
  namespace: speechtotext
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: api-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

#### Manual Scaling Commands
```bash
# Scale API service
kubectl scale deployment/api-service --replicas=5 -n speechtotext

# Scale transcription service
kubectl scale deployment/transcription-service --replicas=3 -n speechtotext

# Check scaling status
kubectl get hpa -n speechtotext
kubectl top pods -n speechtotext
```

### Vertical Scaling

#### Resource Limit Updates
```bash
# Update resource limits for API service
kubectl patch deployment api-service -n speechtotext -p='
{
  "spec": {
    "template": {
      "spec": {
        "containers": [
          {
            "name": "api-service",
            "resources": {
              "requests": {
                "memory": "4Gi",
                "cpu": "2000m"
              },
              "limits": {
                "memory": "8Gi",
                "cpu": "4000m"
              }
            }
          }
        ]
      }
    }
  }
}'
```

### Database Scaling

#### Read Replicas Setup
```bash
#!/bin/bash
# setup-read-replica.sh

# Create read replica
aws rds create-db-instance-read-replica \
    --db-instance-identifier speechtotext-read-replica \
    --source-db-instance-identifier speechtotext-primary \
    --db-instance-class db.r5.large

# Update application configuration to use read replica for queries
kubectl create secret generic db-read-replica \
    --from-literal=url="jdbc:postgresql://speechtotext-read-replica.amazonaws.com:5432/speechtotext"
```

## 🔒 Security Operations

### Security Monitoring

#### Daily Security Checks
```bash
#!/bin/bash
# security-check.sh

echo "🔒 Daily Security Check - $(date)"
echo "================================"

# Check for failed login attempts
echo "1. Failed Authentication Attempts:"
grep "authentication failed" /var/log/auth.log | tail -10

# Check for suspicious network activity
echo -e "\n2. Network Security:"
netstat -an | grep :22 | grep -v ESTABLISHED | wc -l
echo "   Suspicious SSH connections: $?"

# Check SSL certificate status
echo -e "\n3. SSL Certificate Status:"
echo | openssl s_client -servername api.yourdomain.com -connect api.yourdomain.com:443 2>/dev/null | openssl x509 -noout -dates

# Check for security updates
echo -e "\n4. Security Updates Available:"
apt list --upgradable 2>/dev/null | grep -i security | wc -l

# Scan for vulnerabilities
echo -e "\n5. Container Vulnerability Scan:"
trivy image --severity HIGH,CRITICAL ghcr.io/mir00r/speechtotext/api-service:latest | head -20
```

### Access Control Management

#### User Access Audit
```bash
#!/bin/bash
# access-audit.sh

echo "👥 Access Control Audit - $(date)"
echo "================================"

# List all users with sudo access
echo "1. Users with sudo access:"
grep -Po '^sudo.+:\K.*$' /etc/group

# Check SSH key access
echo -e "\n2. SSH Key Access:"
for user in $(awk -F: '$3 >= 1000 {print $1}' /etc/passwd); do
    if [ -f "/home/$user/.ssh/authorized_keys" ]; then
        echo "User $user has SSH keys configured"
        wc -l "/home/$user/.ssh/authorized_keys"
    fi
done

# Check API keys rotation
echo -e "\n3. API Keys Age:"
kubectl get secrets -n speechtotext -o jsonpath='{.items[*].metadata.creationTimestamp}'
```

### Incident Response Playbooks

#### Security Incident Response
```bash
#!/bin/bash
# security-incident.sh

echo "🚨 SECURITY INCIDENT RESPONSE"
echo "Time: $(date)"

# 1. Immediate containment
echo "1. Immediate Actions:"
echo "   - Block suspicious IPs: iptables -A INPUT -s SUSPICIOUS_IP -j DROP"
echo "   - Rotate API keys: kubectl delete secret speechtotext-secrets"
echo "   - Check for compromise: grep -i 'unauthorized\|breach\|hack' /var/log/*"

# 2. Evidence collection
echo -e "\n2. Evidence Collection:"
echo "   - Collect logs: tar -czf incident-logs-$(date +%Y%m%d).tar.gz /var/log/"
echo "   - Network connections: netstat -an > network-$(date +%Y%m%d).txt"
echo "   - Process list: ps aux > processes-$(date +%Y%m%d).txt"

# 3. Communication
echo -e "\n3. Notification:"
echo "   - Notify security team"
echo "   - Document incident in security log"
echo "   - Prepare customer communication if data affected"
```

## 📊 Performance Tuning

### Database Performance Optimization

#### Query Performance Analysis
```sql
-- Check slow queries
SELECT 
    query,
    calls,
    total_exec_time,
    rows,
    mean_exec_time,
    stddev_exec_time
FROM pg_stat_statements 
ORDER BY mean_exec_time DESC 
LIMIT 20;

-- Check index usage
SELECT 
    t.tablename,
    indexname,
    c.reltuples AS num_rows,
    pg_size_pretty(pg_relation_size(quote_ident(t.tablename)::text)) AS table_size,
    pg_size_pretty(pg_relation_size(quote_ident(indexrelname)::text)) AS index_size,
    CASE WHEN indisunique THEN 'Y' ELSE 'N' END AS UNIQUE,
    idx_scan AS number_of_scans,
    idx_tup_read AS tuples_read,
    idx_tup_fetch AS tuples_fetched
FROM pg_tables t
LEFT OUTER JOIN pg_class c ON c.relname=t.tablename
LEFT OUTER JOIN pg_indexes ON c.relname=pg_indexes.tablename
LEFT OUTER JOIN pg_stat_user_indexes psui ON psui.indexrelname=pg_indexes.indexname
WHERE t.schemaname='public'
ORDER BY pg_relation_size(quote_ident(indexrelname)::text) DESC;
```

#### Database Configuration Tuning
```ini
# postgresql.conf optimizations

# Memory settings
shared_buffers = 25% of RAM
effective_cache_size = 75% of RAM  
work_mem = (Total RAM - shared_buffers) / (16 * max_connections)

# Checkpoint settings
checkpoint_completion_target = 0.9
wal_buffers = 16MB

# Connection settings
max_connections = 200
```

### Application Performance Tuning

#### JVM Tuning for API Service
```bash
# JVM flags for production
JAVA_OPTS="
-Xms4g 
-Xmx8g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+ParallelRefProcEnabled
-XX:+UseStringDeduplication
-XX:+PrintGC
-XX:+PrintGCDetails
-XX:+PrintGCTimeStamps
-Xloggc:/var/log/gc.log
-XX:+UseGCLogFileRotation
-XX:NumberOfGCLogFiles=5
-XX:GCLogFileSize=10M
"
```

#### Connection Pool Tuning
```yaml
# application-prod.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

## 🔍 Troubleshooting

### Common Issues and Solutions

#### 1. High Memory Usage
```bash
# Identify memory-consuming processes
ps aux --sort=-%mem | head -20

# Check for memory leaks in Java applications
jmap -histo $(pgrep java) | head -20

# Monitor garbage collection
jstat -gc $(pgrep java) 5s
```

#### 2. Database Connection Issues
```bash
# Check active connections
psql -h postgres-host -U speechtotext -c "
SELECT count(*) as active_connections, state 
FROM pg_stat_activity 
GROUP BY state;"

# Kill long-running queries
psql -h postgres-host -U speechtotext -c "
SELECT pg_terminate_backend(pid) 
FROM pg_stat_activity 
WHERE state = 'active' AND query_start < now() - interval '10 minutes';"
```

#### 3. Disk Space Issues
```bash
# Check disk usage
df -h

# Find large files
find / -type f -size +100M -exec ls -lh {} \; 2>/dev/null | head -20

# Clean up Docker
docker system prune -af
docker volume prune -f

# Clean up logs
journalctl --vacuum-time=7d
find /var/log -name "*.log*" -mtime +7 -delete
```

#### 4. Network Connectivity Issues
```bash
# Test service connectivity
curl -v http://api.yourdomain.com/actuator/health
curl -v http://transcription.yourdomain.com/health

# Check DNS resolution
nslookup api.yourdomain.com

# Test database connectivity
pg_isready -h postgres-host -p 5432 -U speechtotext

# Check firewall rules
iptables -L -n
```

### Emergency Procedures

#### Service Recovery Commands
```bash
# Restart all services
docker-compose restart

# Restart specific service
docker-compose restart api-service

# Check service logs
docker-compose logs -f api-service
docker-compose logs -f transcription-service

# Kubernetes equivalents
kubectl rollout restart deployment/api-service -n speechtotext
kubectl logs -f deployment/api-service -n speechtotext
```

#### Quick Fixes
```bash
# Clear cache
redis-cli FLUSHALL

# Restart database connections
sudo systemctl restart postgresql

# Clear temporary files
rm -rf /tmp/transcription-*
```

---

This operations manual should be kept up-to-date and regularly reviewed by the operations team. Regular drills should be conducted to ensure all procedures work as expected.

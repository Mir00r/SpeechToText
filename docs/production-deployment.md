# Production Deployment Guide

This document provides comprehensive instructions for deploying the Speech to Text service in production environments.

## 📋 Table of Contents

- [Infrastructure Requirements](#infrastructure-requirements)
- [Environment Setup](#environment-setup)
- [Database Configuration](#database-configuration)
- [Container Deployment](#container-deployment)
- [Kubernetes Deployment](#kubernetes-deployment)
- [Load Balancing](#load-balancing)
- [SSL/TLS Configuration](#ssltls-configuration)
- [Monitoring Setup](#monitoring-setup)
- [Backup and Recovery](#backup-and-recovery)
- [Security Hardening](#security-hardening)

## 🏗️ Infrastructure Requirements

### Minimum Production Requirements

#### API Service
- **CPU**: 4 vCPU cores
- **RAM**: 8GB RAM
- **Storage**: 50GB SSD
- **Network**: 1 Gbps
- **Instances**: 2+ (for high availability)

#### Transcription Service
- **CPU**: 8 vCPU cores (or 1x NVIDIA GPU with 8GB+ VRAM)
- **RAM**: 16GB RAM (32GB recommended with GPU)
- **Storage**: 100GB SSD (for model storage and temp files)
- **Network**: 1 Gbps
- **Instances**: 2+ (for high availability)

#### Database (PostgreSQL)
- **CPU**: 4 vCPU cores
- **RAM**: 16GB RAM
- **Storage**: 500GB SSD with IOPS provisioning
- **Backup**: Daily automated backups with 30-day retention
- **Replication**: Read replicas for scaling

#### Object Storage (S3/MinIO)
- **Storage**: 10TB+ (based on usage)
- **Redundancy**: Multi-AZ replication
- **Backup**: Cross-region replication recommended
- **Access**: CDN integration for faster downloads

### Recommended Production Architecture

```
                    ┌─────────────────┐
                    │   Load Balancer │
                    │   (HAProxy/ALB) │
                    └─────────────────┘
                             │
                ┌────────────┼────────────┐
                │                         │
        ┌───────▼───────┐         ┌───────▼───────┐
        │ API Service   │         │ API Service   │
        │ Instance 1    │         │ Instance 2    │
        └───────┬───────┘         └───────┬───────┘
                │                         │
                └────────────┬────────────┘
                             │
              ┌──────────────▼──────────────┐
              │                             │
      ┌───────▼───────┐             ┌───────▼───────┐
      │ Transcription │             │ Transcription │
      │ Service 1     │             │ Service 2     │
      └───────┬───────┘             └───────┬───────┘
              │                             │
              └──────────────┬──────────────┘
                             │
          ┌──────────────────┼──────────────────┐
          │                  │                  │
  ┌───────▼───────┐  ┌───────▼───────┐  ┌───────▼───────┐
  │   PostgreSQL  │  │   Object      │  │   Monitoring  │
  │   Primary     │  │   Storage     │  │   Stack       │
  │               │  │   (S3/MinIO)  │  │               │
  └───────────────┘  └───────────────┘  └───────────────┘
```

## ⚙️ Environment Setup

### Environment Variables

Create production environment configuration files:

#### API Service (`api-service.env`)
```bash
# Application Configuration
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
LOGGING_LEVEL_COM_SPEECHTOTEXT=INFO

# Database Configuration
DATABASE_URL=jdbc:postgresql://postgres-primary:5432/speechtotext
DATABASE_USERNAME=speechtotext
DATABASE_PASSWORD=${DB_PASSWORD}
DATABASE_CONNECTION_POOL_SIZE=20
DATABASE_CONNECTION_TIMEOUT=30000

# S3/Object Storage Configuration
S3_ENDPOINT=https://s3.your-region.amazonaws.com
S3_ACCESS_KEY=${S3_ACCESS_KEY}
S3_SECRET_KEY=${S3_SECRET_KEY}
S3_BUCKET_NAME=speechtotext-prod
S3_REGION=us-east-1

# Transcription Service Configuration
TRANSCRIPTION_SERVICE_URL=http://transcription-service:8081
CALLBACK_BASE_URL=https://api.yourdomain.com

# Sync Processing Configuration
SYNC_THRESHOLD=30
SYNC_TIMEOUT=60

# Security Configuration
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,metrics,prometheus
MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=when_authorized
MANAGEMENT_SERVER_PORT=8081

# Rate Limiting
RATE_LIMIT_REQUESTS_PER_MINUTE=100
RATE_LIMIT_REQUESTS_PER_HOUR=1000
```

#### Transcription Service (`transcription-service.env`)
```bash
# Application Configuration
ENV=production
PORT=8081
WORKERS=4
LOG_LEVEL=INFO

# S3/Object Storage Configuration
S3_ENDPOINT=https://s3.your-region.amazonaws.com
S3_ACCESS_KEY=${S3_ACCESS_KEY}
S3_SECRET_KEY=${S3_SECRET_KEY}
S3_BUCKET_NAME=speechtotext-prod
S3_REGION=us-east-1

# WhisperX Configuration
WHISPER_MODEL_DIR=/app/models
HUGGINGFACE_TOKEN=${HF_TOKEN}
COMPUTE_TYPE=float16
DEVICE=cuda  # or cpu
BATCH_SIZE=16

# Performance Configuration
MAX_WORKERS=4
CALLBACK_TIMEOUT=30
CALLBACK_RETRIES=3
```

## 🗄️ Database Configuration

### PostgreSQL Production Setup

#### 1. Database Instance Configuration
```sql
-- Create production database and user
CREATE DATABASE speechtotext;
CREATE USER speechtotext WITH ENCRYPTED PASSWORD 'your-secure-password';
GRANT ALL PRIVILEGES ON DATABASE speechtotext TO speechtotext;
GRANT ALL ON SCHEMA public TO speechtotext;

-- Configure connection limits
ALTER USER speechtotext CONNECTION LIMIT 50;
```

#### 2. PostgreSQL Configuration (`postgresql.conf`)
```ini
# Connection Settings
max_connections = 200
shared_buffers = 4GB
effective_cache_size = 12GB
maintenance_work_mem = 1GB
checkpoint_completion_target = 0.9
wal_buffers = 16MB
default_statistics_target = 100
random_page_cost = 1.1
effective_io_concurrency = 200

# Write Ahead Log
wal_level = replica
max_wal_senders = 3
max_replication_slots = 3
hot_standby = on
hot_standby_feedback = on

# Logging
log_destination = 'csvlog'
logging_collector = on
log_directory = 'logs'
log_filename = 'postgresql-%Y%m%d.log'
log_statement = 'mod'
log_min_duration_statement = 1000
```

#### 3. Database Monitoring Queries
```sql
-- Monitor active connections
SELECT count(*) as active_connections, state 
FROM pg_stat_activity 
GROUP BY state;

-- Monitor table sizes
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Monitor slow queries
SELECT query, mean_exec_time, calls, total_exec_time
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;
```

## 🐳 Container Deployment

### Docker Compose Production Configuration

Create `docker-compose.prod.yml`:

```yaml
version: '3.8'

services:
  # API Service
  api-service:
    image: ghcr.io/mir00r/speechtotext/api-service:${VERSION}
    deploy:
      replicas: 2
      resources:
        limits:
          cpus: '2.0'
          memory: 4G
        reservations:
          cpus: '1.0'
          memory: 2G
      restart_policy:
        condition: on-failure
        delay: 10s
        max_attempts: 3
      update_config:
        parallelism: 1
        delay: 30s
        order: stop-first
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    env_file:
      - api-service.env
    ports:
      - "8080:8080"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    networks:
      - speechtotext
    depends_on:
      - postgres
      - minio

  # Transcription Service
  transcription-service:
    image: ghcr.io/mir00r/speechtotext/transcription-service:${VERSION}
    deploy:
      replicas: 2
      resources:
        limits:
          cpus: '4.0'
          memory: 8G
        reservations:
          cpus: '2.0'
          memory: 4G
      restart_policy:
        condition: on-failure
        delay: 10s
        max_attempts: 3
    environment:
      - ENV=production
    env_file:
      - transcription-service.env
    ports:
      - "8081:8081"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 120s
    networks:
      - speechtotext
    depends_on:
      - postgres
      - minio
    volumes:
      - whisper-models:/app/models

  # PostgreSQL Database
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: speechtotext
      POSTGRES_USER: speechtotext
      POSTGRES_PASSWORD: ${DB_PASSWORD}
      POSTGRES_INITDB_ARGS: "--auth-host=scram-sha-256"
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./postgres-init:/docker-entrypoint-initdb.d
    ports:
      - "5432:5432"
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 4G
        reservations:
          cpus: '1.0'
          memory: 2G
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U speechtotext -d speechtotext"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - speechtotext

  # MinIO Object Storage
  minio:
    image: minio/minio:RELEASE.2023-12-07T04-16-00Z
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: ${MINIO_ROOT_USER}
      MINIO_ROOT_PASSWORD: ${MINIO_ROOT_PASSWORD}
    volumes:
      - minio-data:/data
    ports:
      - "9000:9000"
      - "9001:9001"
    deploy:
      resources:
        limits:
          memory: 2G
        reservations:
          memory: 512M
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - speechtotext

  # Load Balancer
  haproxy:
    image: haproxy:2.8
    volumes:
      - ./haproxy.cfg:/usr/local/etc/haproxy/haproxy.cfg:ro
    ports:
      - "80:80"
      - "443:443"
      - "8404:8404"  # Stats page
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
    networks:
      - speechtotext
    depends_on:
      - api-service

volumes:
  postgres-data:
    driver: local
  minio-data:
    driver: local
  whisper-models:
    driver: local

networks:
  speechtotext:
    driver: overlay
    attachable: true
```

### HAProxy Configuration

Create `haproxy.cfg`:

```ini
global
    daemon
    log stdout local0
    chroot /var/lib/haproxy
    stats socket /run/haproxy/admin.sock mode 660 level admin
    stats timeout 30s
    user haproxy
    group haproxy

defaults
    mode http
    log global
    option httplog
    option dontlognull
    option log-health-checks
    option forwardfor
    option http-server-close
    timeout connect 5000
    timeout client 50000
    timeout server 50000
    timeout http-request 10s
    timeout http-keep-alive 2s
    timeout check 10s
    errorfile 400 /etc/haproxy/errors/400.http
    errorfile 403 /etc/haproxy/errors/403.http
    errorfile 408 /etc/haproxy/errors/408.http
    errorfile 500 /etc/haproxy/errors/500.http
    errorfile 502 /etc/haproxy/errors/502.http
    errorfile 503 /etc/haproxy/errors/503.http
    errorfile 504 /etc/haproxy/errors/504.http

# Stats page
listen stats
    bind *:8404
    stats enable
    stats uri /stats
    stats refresh 30s
    stats admin if LOCALHOST

# API Service Frontend
frontend api_frontend
    bind *:80
    bind *:443 ssl crt /etc/ssl/certs/speechtotext.pem
    redirect scheme https if !{ ssl_fc }
    
    # Security headers
    http-response set-header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload"
    http-response set-header X-Frame-Options "SAMEORIGIN"
    http-response set-header X-Content-Type-Options "nosniff"
    http-response set-header X-XSS-Protection "1; mode=block"
    
    # Rate limiting
    stick-table type ip size 100k expire 30s store http_req_rate(10s)
    http-request track-sc0 src
    http-request reject if { sc_http_req_rate(0) gt 20 }
    
    default_backend api_backend

# API Service Backend
backend api_backend
    balance roundrobin
    option httpchk GET /actuator/health
    
    server api1 api-service:8080 check inter 5s rise 2 fall 3
    server api2 api-service:8080 check inter 5s rise 2 fall 3
```

## ☸️ Kubernetes Deployment

### Namespace and ConfigMap

```yaml
# namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: speechtotext
  labels:
    app: speechtotext
    environment: production

---
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: speechtotext-config
  namespace: speechtotext
data:
  api-service.properties: |
    spring.profiles.active=prod
    logging.level.com.speechtotext=INFO
    management.endpoints.web.exposure.include=health,metrics,prometheus
    
  transcription-service.properties: |
    ENV=production
    LOG_LEVEL=INFO
    WORKERS=4
```

### Secrets Management

```yaml
# secrets.yaml
apiVersion: v1
kind: Secret
metadata:
  name: speechtotext-secrets
  namespace: speechtotext
type: Opaque
data:
  db-password: <base64-encoded-password>
  s3-access-key: <base64-encoded-access-key>
  s3-secret-key: <base64-encoded-secret-key>
  hf-token: <base64-encoded-huggingface-token>
```

### API Service Deployment

```yaml
# api-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-service
  namespace: speechtotext
  labels:
    app: api-service
    version: v1
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 1
      maxSurge: 1
  selector:
    matchLabels:
      app: api-service
  template:
    metadata:
      labels:
        app: api-service
        version: v1
    spec:
      containers:
      - name: api-service
        image: ghcr.io/mir00r/speechtotext/api-service:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: speechtotext-secrets
              key: db-password
        - name: S3_ACCESS_KEY
          valueFrom:
            secretKeyRef:
              name: speechtotext-secrets
              key: s3-access-key
        - name: S3_SECRET_KEY
          valueFrom:
            secretKeyRef:
              name: speechtotext-secrets
              key: s3-secret-key
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
          successThreshold: 1
          failureThreshold: 3
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 120
          periodSeconds: 30
          timeoutSeconds: 10
          failureThreshold: 3

---
apiVersion: v1
kind: Service
metadata:
  name: api-service
  namespace: speechtotext
  labels:
    app: api-service
spec:
  selector:
    app: api-service
  ports:
  - port: 8080
    targetPort: 8080
    protocol: TCP
  type: ClusterIP
```

### Transcription Service Deployment

```yaml
# transcription-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: transcription-service
  namespace: speechtotext
  labels:
    app: transcription-service
    version: v1
spec:
  replicas: 2
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 0
      maxSurge: 1
  selector:
    matchLabels:
      app: transcription-service
  template:
    metadata:
      labels:
        app: transcription-service
        version: v1
    spec:
      containers:
      - name: transcription-service
        image: ghcr.io/mir00r/speechtotext/transcription-service:latest
        ports:
        - containerPort: 8081
        env:
        - name: ENV
          value: "production"
        - name: S3_ACCESS_KEY
          valueFrom:
            secretKeyRef:
              name: speechtotext-secrets
              key: s3-access-key
        - name: S3_SECRET_KEY
          valueFrom:
            secretKeyRef:
              name: speechtotext-secrets
              key: s3-secret-key
        - name: HUGGINGFACE_TOKEN
          valueFrom:
            secretKeyRef:
              name: speechtotext-secrets
              key: hf-token
        resources:
          requests:
            memory: "4Gi"
            cpu: "2000m"
            nvidia.com/gpu: 1
          limits:
            memory: "8Gi"
            cpu: "4000m"
            nvidia.com/gpu: 1
        readinessProbe:
          httpGet:
            path: /health
            port: 8081
          initialDelaySeconds: 120
          periodSeconds: 15
          timeoutSeconds: 10
          successThreshold: 1
          failureThreshold: 3
        livenessProbe:
          httpGet:
            path: /health
            port: 8081
          initialDelaySeconds: 180
          periodSeconds: 30
          timeoutSeconds: 15
          failureThreshold: 3
        volumeMounts:
        - name: model-cache
          mountPath: /app/models
      volumes:
      - name: model-cache
        persistentVolumeClaim:
          claimName: whisper-models-pvc

---
apiVersion: v1
kind: Service
metadata:
  name: transcription-service
  namespace: speechtotext
  labels:
    app: transcription-service
spec:
  selector:
    app: transcription-service
  ports:
  - port: 8081
    targetPort: 8081
    protocol: TCP
  type: ClusterIP

---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: whisper-models-pvc
  namespace: speechtotext
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 50Gi
  storageClassName: gp2
```

### Ingress Configuration

```yaml
# ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: speechtotext-ingress
  namespace: speechtotext
  annotations:
    kubernetes.io/ingress.class: "nginx"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
    nginx.ingress.kubernetes.io/rate-limit: "100"
    nginx.ingress.kubernetes.io/rate-limit-window: "1m"
    nginx.ingress.kubernetes.io/client-max-body-size: "100m"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "300"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "300"
spec:
  tls:
  - hosts:
    - api.yourdomain.com
    secretName: speechtotext-tls
  rules:
  - host: api.yourdomain.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: api-service
            port:
              number: 8080
```

## 📊 Monitoring Setup

See the detailed [Monitoring and Observability Guide](./monitoring.md) for comprehensive monitoring setup including Prometheus, Grafana, and alerting configurations.

## 🔐 Security Hardening

### Application Security
- Enable HTTPS/TLS encryption for all communications
- Implement proper authentication and authorization
- Configure rate limiting and request throttling
- Enable CORS with strict origin policies
- Use secure headers (HSTS, CSP, X-Frame-Options)
- Regular security updates and vulnerability scanning

### Infrastructure Security
- Network segmentation and firewall rules
- Regular security patches and updates
- Encrypted storage and backups
- Access control and audit logging
- Secret management with rotation policies
- Container security scanning

## 🔄 Backup and Recovery

### Database Backups
```bash
# Daily backup script
#!/bin/bash
BACKUP_DIR="/backups/postgresql"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="speechtotext_backup_${DATE}.sql"

pg_dump -h postgres-host -U speechtotext -d speechtotext > "${BACKUP_DIR}/${BACKUP_FILE}"
gzip "${BACKUP_DIR}/${BACKUP_FILE}"

# Retention policy: keep daily backups for 30 days
find ${BACKUP_DIR} -name "*.gz" -type f -mtime +30 -delete
```

### Object Storage Backups
- Configure cross-region replication for S3/MinIO
- Implement versioning for uploaded files
- Regular backup verification and restore testing

## 🚀 Deployment Checklist

### Pre-deployment
- [ ] Infrastructure provisioned and configured
- [ ] SSL certificates obtained and installed
- [ ] Database initialized with proper permissions
- [ ] Object storage buckets created
- [ ] Environment variables and secrets configured
- [ ] Load balancer and DNS configured
- [ ] Monitoring and alerting setup
- [ ] Backup procedures tested

### Deployment
- [ ] Deploy database migrations
- [ ] Deploy application containers
- [ ] Verify health checks pass
- [ ] Run smoke tests
- [ ] Verify monitoring and metrics
- [ ] Update DNS if needed
- [ ] Update documentation

### Post-deployment
- [ ] Monitor application metrics
- [ ] Verify backup procedures
- [ ] Update monitoring dashboards
- [ ] Document any issues or changes
- [ ] Plan for scaling if needed

## 📞 Support and Maintenance

For ongoing support and maintenance, refer to:
- [Operations Manual](./operations.md)
- [Troubleshooting Guide](./troubleshooting.md)
- [Performance Tuning](./performance-tuning.md)
- [Scaling Guide](./scaling.md)

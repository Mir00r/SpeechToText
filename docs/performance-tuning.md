# Performance Tuning Guide

Comprehensive performance optimization guide for the Speech to Text service, covering all components from infrastructure to application-level optimizations.

## 📋 Table of Contents

- [Performance Overview](#performance-overview)
- [System Requirements](#system-requirements)
- [Application-Level Optimization](#application-level-optimization)
- [Database Performance Tuning](#database-performance-tuning)
- [Storage Optimization](#storage-optimization)
- [Network and Infrastructure](#network-and-infrastructure)
- [WhisperX Model Optimization](#whisperx-model-optimization)
- [Monitoring and Profiling](#monitoring-and-profiling)
- [Scaling Strategies](#scaling-strategies)
- [Performance Testing](#performance-testing)

## 🎯 Performance Overview

### Current Performance Metrics
- **API Response Time**: < 100ms (95th percentile)
- **Transcription Speed**: 0.1-0.5x real-time (depending on model)
- **Concurrent Transcriptions**: 10-50 (depending on resources)
- **Throughput**: 100-500 requests/minute
- **Availability**: 99.9%

### Performance Targets
- **API Response Time**: < 50ms (95th percentile)
- **Transcription Speed**: 0.5-2x real-time
- **Concurrent Transcriptions**: 100+
- **Throughput**: 1000+ requests/minute
- **Availability**: 99.95%

## 💻 System Requirements

### Minimum Requirements
```yaml
CPU: 4 cores, 2.4GHz
RAM: 8GB
Storage: 100GB SSD
Network: 1Gbps
```

### Recommended Requirements
```yaml
CPU: 8+ cores, 3.0GHz+ (Intel Xeon or AMD EPYC)
RAM: 32GB+ DDR4
Storage: 500GB+ NVMe SSD
Network: 10Gbps
GPU: NVIDIA RTX 3080+ or Tesla V100+ (for GPU acceleration)
```

### Production Requirements
```yaml
CPU: 16+ cores, 3.2GHz+ (Intel Xeon or AMD EPYC)
RAM: 64GB+ DDR4 ECC
Storage: 1TB+ NVMe SSD RAID
Network: 10Gbps+ with redundancy
GPU: Multiple NVIDIA A100 or H100 GPUs
Load Balancer: Dedicated hardware or cloud LB
```

## 🚀 Application-Level Optimization

### Spring Boot API Service

#### JVM Tuning
```yaml
# docker-compose.yml
api-service:
  environment:
    - JAVA_OPTS=-Xms2g -Xmx4g 
                -XX:+UseG1GC 
                -XX:G1HeapRegionSize=16m
                -XX:+UseStringDeduplication
                -XX:+OptimizeStringConcat
                -XX:MaxGCPauseMillis=200
                -XX:ParallelGCThreads=8
                -XX:ConcGCThreads=2
                -XX:InitiatingHeapOccupancyPercent=45
                -XX:+HeapDumpOnOutOfMemoryError
                -XX:HeapDumpPath=/logs/heap_dump.hprof
```

#### Connection Pool Optimization
```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-timeout: 20000
      leak-detection-threshold: 60000
      
  # Redis connection pool (if using Redis)
  redis:
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: -1ms
```

#### Async Processing Optimization
```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    @Bean("transcriptionExecutor")
    public TaskExecutor transcriptionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("transcription-");
        executor.setRejectedExecutionHandler(new CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
    
    @Bean("webhookExecutor") 
    public TaskExecutor webhookExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("webhook-");
        executor.initialize();
        return executor;
    }
}
```

#### Caching Strategy
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .recordStats());
        return cacheManager;
    }
}

@Service
public class TranscriptionService {
    
    @Cacheable(value = "transcriptions", key = "#id")
    public TranscriptionJob getTranscription(String id) {
        return transcriptionRepository.findById(id);
    }
    
    @Cacheable(value = "models", key = "#model")
    public ModelInfo getModelInfo(String model) {
        return modelRepository.findByName(model);
    }
}
```

### Python Transcription Service

#### FastAPI Optimization
```python
# main.py
from fastapi import FastAPI
from contextlib import asynccontextmanager
import uvloop
import asyncio

# Use uvloop for better async performance
asyncio.set_event_loop_policy(uvloop.EventLoopPolicy())

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    await load_models()
    yield
    # Shutdown
    await cleanup_models()

app = FastAPI(
    title="Transcription Service",
    lifespan=lifespan
)

# Optimize JSON serialization
from orjson import dumps
app.add_middleware(
    ORJSONMiddleware
)
```

#### Model Loading Optimization
```python
# models/whisper_manager.py
import asyncio
import torch
from concurrent.futures import ThreadPoolExecutor
from functools import lru_cache

class WhisperModelManager:
    def __init__(self):
        self.models = {}
        self.model_lock = asyncio.Lock()
        self.executor = ThreadPoolExecutor(max_workers=4)
        
    @lru_cache(maxsize=5)
    def _load_model_sync(self, model_name: str, device: str):
        """Synchronous model loading with caching"""
        import whisperx
        return whisperx.load_model(
            model_name, 
            device=device,
            compute_type="float16" if device == "cuda" else "int8"
        )
    
    async def get_model(self, model_name: str):
        """Async model retrieval"""
        if model_name not in self.models:
            async with self.model_lock:
                if model_name not in self.models:
                    # Load model in thread pool to avoid blocking
                    loop = asyncio.get_event_loop()
                    device = "cuda" if torch.cuda.is_available() else "cpu"
                    
                    self.models[model_name] = await loop.run_in_executor(
                        self.executor,
                        self._load_model_sync,
                        model_name,
                        device
                    )
        
        return self.models[model_name]
```

#### Memory Management
```python
# worker/transcribe.py
import gc
import torch
from memory_profiler import profile

class TranscriptionWorker:
    
    def __init__(self):
        self.max_memory_usage = 0.8  # 80% of available memory
        
    @profile
    async def transcribe_audio(self, audio_path: str, model_name: str):
        try:
            # Monitor memory before processing
            self._check_memory_usage()
            
            # Load model
            model = await self.model_manager.get_model(model_name)
            
            # Process audio
            result = self._process_with_chunking(audio_path, model)
            
            return result
            
        finally:
            # Clean up memory
            self._cleanup_memory()
    
    def _check_memory_usage(self):
        """Check and manage memory usage"""
        import psutil
        
        memory = psutil.virtual_memory()
        if memory.percent > self.max_memory_usage * 100:
            self._force_cleanup()
            
    def _cleanup_memory(self):
        """Force memory cleanup"""
        gc.collect()
        if torch.cuda.is_available():
            torch.cuda.empty_cache()
            torch.cuda.synchronize()
    
    def _process_with_chunking(self, audio_path: str, model, chunk_size: int = 30):
        """Process audio in chunks to manage memory"""
        # Implementation for chunked processing
        pass
```

#### Batch Processing
```python
# worker/batch_processor.py
import asyncio
from typing import List, Dict
from dataclasses import dataclass

@dataclass
class TranscriptionJob:
    id: str
    audio_path: str
    model_name: str
    language: str

class BatchProcessor:
    def __init__(self, batch_size: int = 8):
        self.batch_size = batch_size
        self.queue = asyncio.Queue()
        self.processing = False
        
    async def add_job(self, job: TranscriptionJob):
        """Add job to processing queue"""
        await self.queue.put(job)
        
        if not self.processing:
            asyncio.create_task(self._process_batches())
    
    async def _process_batches(self):
        """Process jobs in batches"""
        self.processing = True
        
        try:
            while not self.queue.empty():
                batch = []
                
                # Collect jobs for batch
                for _ in range(min(self.batch_size, self.queue.qsize())):
                    batch.append(await self.queue.get())
                
                # Group by model for efficiency
                batches_by_model = self._group_by_model(batch)
                
                # Process each model batch
                tasks = []
                for model_name, jobs in batches_by_model.items():
                    task = asyncio.create_task(
                        self._process_model_batch(model_name, jobs)
                    )
                    tasks.append(task)
                
                await asyncio.gather(*tasks)
                
        finally:
            self.processing = False
    
    def _group_by_model(self, jobs: List[TranscriptionJob]) -> Dict[str, List[TranscriptionJob]]:
        """Group jobs by model for batch processing"""
        grouped = {}
        for job in jobs:
            if job.model_name not in grouped:
                grouped[job.model_name] = []
            grouped[job.model_name].append(job)
        return grouped
```

## 🗄️ Database Performance Tuning

### PostgreSQL Optimization

#### Configuration Tuning
```sql
-- postgresql.conf optimizations
-- Memory settings
shared_buffers = '8GB'                    -- 25% of RAM
effective_cache_size = '24GB'             -- 75% of RAM  
work_mem = '256MB'                        -- Per connection work memory
maintenance_work_mem = '2GB'              -- For maintenance operations

-- Connection settings
max_connections = 200
superuser_reserved_connections = 3

-- Query planning
random_page_cost = 1.1                    -- For SSD storage
effective_io_concurrency = 200            -- For SSD storage

-- Write-ahead logging
wal_buffers = '16MB'
checkpoint_completion_target = 0.9
max_wal_size = '4GB'
min_wal_size = '1GB'

-- Background writer
bgwriter_delay = 200ms
bgwriter_lru_maxpages = 100
bgwriter_lru_multiplier = 2.0

-- Statistics
track_activities = on
track_counts = on
track_io_timing = on
track_functions = all
```

#### Index Optimization
```sql
-- Primary indexes for transcription_jobs table
CREATE INDEX CONCURRENTLY idx_transcription_jobs_status 
ON transcription_jobs(status) WHERE status IN ('PENDING', 'PROCESSING');

CREATE INDEX CONCURRENTLY idx_transcription_jobs_created_at 
ON transcription_jobs(created_at DESC);

CREATE INDEX CONCURRENTLY idx_transcription_jobs_user_id 
ON transcription_jobs(user_id);

-- Composite indexes for common queries
CREATE INDEX CONCURRENTLY idx_transcription_jobs_user_status_created
ON transcription_jobs(user_id, status, created_at DESC);

CREATE INDEX CONCURRENTLY idx_transcription_jobs_status_updated
ON transcription_jobs(status, updated_at) 
WHERE status IN ('PENDING', 'PROCESSING');

-- Partial indexes for better performance
CREATE INDEX CONCURRENTLY idx_transcription_jobs_failed
ON transcription_jobs(created_at DESC) 
WHERE status = 'FAILED';

-- Text search indexes
CREATE INDEX CONCURRENTLY idx_transcription_jobs_filename_gin
ON transcription_jobs USING gin(to_tsvector('english', original_filename));
```

#### Query Optimization
```sql
-- Optimized query for listing user transcriptions
EXPLAIN (ANALYZE, BUFFERS) 
SELECT id, original_filename, status, duration_seconds, created_at
FROM transcription_jobs 
WHERE user_id = $1 
  AND status = $2
ORDER BY created_at DESC 
LIMIT $3 OFFSET $4;

-- Optimized query for dashboard metrics
WITH recent_jobs AS (
  SELECT status, COUNT(*) as count
  FROM transcription_jobs 
  WHERE created_at >= NOW() - INTERVAL '24 hours'
  GROUP BY status
)
SELECT 
  COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN count END), 0) as completed,
  COALESCE(SUM(CASE WHEN status = 'FAILED' THEN count END), 0) as failed,
  COALESCE(SUM(CASE WHEN status = 'PROCESSING' THEN count END), 0) as processing
FROM recent_jobs;
```

#### Connection Pool Tuning
```yaml
# Docker Compose PostgreSQL
postgres:
  environment:
    - POSTGRES_MAX_CONNECTIONS=200
    - POSTGRES_SHARED_PRELOAD_LIBRARIES=pg_stat_statements
  command: |
    postgres 
      -c max_connections=200
      -c shared_buffers=2GB
      -c effective_cache_size=6GB
      -c maintenance_work_mem=512MB
      -c checkpoint_completion_target=0.9
      -c wal_buffers=16MB
      -c default_statistics_target=100
      -c random_page_cost=1.1
      -c effective_io_concurrency=200
```

#### Database Monitoring
```sql
-- Monitor slow queries
SELECT 
  query,
  calls,
  total_time,
  mean_time,
  stddev_time,
  rows
FROM pg_stat_statements 
ORDER BY mean_time DESC 
LIMIT 10;

-- Monitor index usage
SELECT 
  indexrelname,
  idx_tup_read,
  idx_tup_fetch,
  idx_scan
FROM pg_stat_user_indexes 
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;

-- Monitor table sizes
SELECT 
  tablename,
  pg_size_pretty(pg_total_relation_size(tablename::regclass)) as size,
  pg_size_pretty(pg_relation_size(tablename::regclass)) as table_size,
  pg_size_pretty(pg_indexes_size(tablename::regclass)) as indexes_size
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(tablename::regclass) DESC;
```

## 💾 Storage Optimization

### MinIO/S3 Performance Tuning

#### MinIO Configuration
```yaml
# docker-compose.yml
minio:
  command: server /data --console-address ":9001"
  environment:
    - MINIO_ROOT_USER=minioadmin
    - MINIO_ROOT_PASSWORD=minioadmin
    - MINIO_STORAGE_CLASS_STANDARD=EC:2  # Erasure coding
    - MINIO_COMPRESS=on                   # Enable compression
    - MINIO_COMPRESS_EXTENSIONS=.wav,.mp3,.m4a,.flac
    - MINIO_COMPRESS_MIME_TYPES=audio/*
  volumes:
    - minio_data:/data:rw
```

#### Upload Optimization
```java
// Spring Boot S3 client optimization
@Configuration
public class S3Config {
    
    @Bean
    public S3AsyncClient s3AsyncClient() {
        return S3AsyncClient.builder()
            .region(Region.US_EAST_1)
            .httpClient(NettyNioAsyncHttpClient.builder()
                .maxConcurrency(100)
                .connectionAcquisitionTimeout(Duration.ofSeconds(10))
                .connectionTimeout(Duration.ofSeconds(10))
                .build())
            .multipartConfiguration(MultipartConfiguration.builder()
                .thresholdInBytes(16 * 1024 * 1024L)    // 16MB threshold
                .minimumPartSizeInBytes(8 * 1024 * 1024L) // 8MB min part size
                .build())
            .build();
    }
}

@Service
public class StorageService {
    
    @Async("storageExecutor")
    public CompletableFuture<String> uploadFile(String key, InputStream inputStream, long contentLength) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentLength(contentLength)
            .storageClass(StorageClass.STANDARD)
            .build();
            
        return s3AsyncClient.putObject(putRequest, AsyncRequestBody.fromInputStream(
            inputStream, contentLength, Executors.newCachedThreadPool()))
            .thenApply(response -> key);
    }
}
```

#### Multipart Upload for Large Files
```python
# Python async multipart upload
import aiofiles
import aioboto3
from concurrent.futures import ThreadPoolExecutor

class AsyncS3Uploader:
    def __init__(self):
        self.session = aioboto3.Session()
        self.executor = ThreadPoolExecutor(max_workers=10)
    
    async def upload_large_file(self, file_path: str, bucket: str, key: str, chunk_size: int = 100 * 1024 * 1024):
        """Upload large file using multipart upload"""
        async with self.session.client('s3') as s3:
            # Initiate multipart upload
            response = await s3.create_multipart_upload(
                Bucket=bucket,
                Key=key,
                StorageClass='STANDARD'
            )
            
            upload_id = response['UploadId']
            parts = []
            
            try:
                async with aiofiles.open(file_path, 'rb') as file:
                    part_number = 1
                    
                    while True:
                        chunk = await file.read(chunk_size)
                        if not chunk:
                            break
                        
                        # Upload part
                        part_response = await s3.upload_part(
                            Bucket=bucket,
                            Key=key,
                            PartNumber=part_number,
                            UploadId=upload_id,
                            Body=chunk
                        )
                        
                        parts.append({
                            'ETag': part_response['ETag'],
                            'PartNumber': part_number
                        })
                        
                        part_number += 1
                
                # Complete multipart upload
                await s3.complete_multipart_upload(
                    Bucket=bucket,
                    Key=key,
                    UploadId=upload_id,
                    MultipartUpload={'Parts': parts}
                )
                
            except Exception as e:
                # Abort multipart upload on error
                await s3.abort_multipart_upload(
                    Bucket=bucket,
                    Key=key,
                    UploadId=upload_id
                )
                raise e
```

## 🌐 Network and Infrastructure

### Load Balancer Optimization

#### HAProxy Configuration
```
# haproxy.cfg - Optimized for high performance
global
    maxconn 40000
    log stdout local0
    chroot /var/lib/haproxy
    stats socket /run/haproxy/admin.sock mode 660
    stats timeout 30s
    user haproxy
    group haproxy
    daemon

defaults
    mode http
    timeout connect 5000ms
    timeout client 50000ms
    timeout server 50000ms
    timeout http-request 10000ms
    timeout http-keep-alive 2000ms
    timeout queue 5000ms
    timeout tunnel 2m
    timeout client-fin 1000ms
    timeout server-fin 1000ms
    
    # Enable compression
    compression algo gzip
    compression type text/html text/plain text/css text/javascript application/javascript application/json

    # Connection reuse
    option http-keep-alive
    option httpclose

backend api_servers
    balance roundrobin
    option httpchk GET /actuator/health
    http-check expect status 200
    
    # Connection pooling
    option http-reuse aggressive
    
    # Health check optimization  
    default-server check inter 10s rise 3 fall 2
    
    server api1 api-service-1:8080 maxconn 1000
    server api2 api-service-2:8080 maxconn 1000
    server api3 api-service-3:8080 maxconn 1000

frontend api_frontend
    bind *:80
    bind *:443 ssl crt /etc/ssl/certs/yourdomain.pem
    redirect scheme https if !{ ssl_fc }
    
    # Rate limiting
    stick-table type ip size 100k expire 30s store http_req_rate(10s)
    http-request track-sc0 src
    http-request deny if { sc_http_req_rate(0) gt 20 }
    
    # Optimize for API traffic
    http-request set-header X-Forwarded-Proto https if { ssl_fc }
    http-request set-header X-Forwarded-For %[src]
    
    default_backend api_servers
```

#### NGINX Optimization (Alternative)
```nginx
# nginx.conf - High performance configuration
worker_processes auto;
worker_cpu_affinity auto;
worker_rlimit_nofile 100000;

events {
    worker_connections 4000;
    use epoll;
    multi_accept on;
}

http {
    # Basic optimization
    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 30;
    keepalive_requests 1000;
    
    # Gzip compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css text/xml text/javascript 
               application/javascript application/xml+rss 
               application/json application/xml;
    
    # Connection pooling
    upstream api_backend {
        keepalive 32;
        keepalive_requests 1000;
        keepalive_timeout 60s;
        
        server api-service-1:8080 max_fails=3 fail_timeout=30s;
        server api-service-2:8080 max_fails=3 fail_timeout=30s;
        server api-service-3:8080 max_fails=3 fail_timeout=30s;
    }
    
    # Rate limiting
    limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
    
    server {
        listen 80;
        server_name api.yourdomain.com;
        
        # Rate limiting
        limit_req zone=api burst=20 nodelay;
        
        # Proxy settings
        location / {
            proxy_pass http://api_backend;
            proxy_http_version 1.1;
            proxy_set_header Connection "";
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            
            # Timeouts
            proxy_connect_timeout 5s;
            proxy_send_timeout 60s;
            proxy_read_timeout 60s;
            
            # Buffering
            proxy_buffering on;
            proxy_buffer_size 128k;
            proxy_buffers 4 256k;
            proxy_busy_buffers_size 256k;
        }
        
        # Health check endpoint
        location /health {
            access_log off;
            return 200 "healthy\n";
        }
    }
}
```

### CDN Configuration
```yaml
# CloudFlare or AWS CloudFront configuration
cache_control_headers:
  # Static assets
  "*.js": "public, max-age=31536000, immutable"
  "*.css": "public, max-age=31536000, immutable"
  "*.png": "public, max-age=31536000, immutable"
  
  # API responses
  "/api/v1/health": "public, max-age=30"
  "/api/v1/models": "public, max-age=3600"
  "/api/v1/languages": "public, max-age=3600"
  
  # Transcription results (temporary caching)
  "/api/v1/transcriptions/*/download": "private, max-age=300"

compression:
  enabled: true
  types: ["text/*", "application/json", "application/javascript"]
  
minification:
  html: true
  css: true 
  js: true
```

## 🎤 WhisperX Model Optimization

### Model Selection Strategy
```python
# Dynamic model selection based on file characteristics
class ModelSelector:
    def __init__(self):
        self.model_performance = {
            'tiny': {'speed': 10.0, 'memory': 1, 'accuracy': 0.7},
            'base': {'speed': 5.0, 'memory': 2, 'accuracy': 0.8},
            'small': {'speed': 3.0, 'memory': 4, 'accuracy': 0.85},
            'medium': {'speed': 2.0, 'memory': 8, 'accuracy': 0.9},
            'large': {'speed': 1.0, 'memory': 16, 'accuracy': 0.95}
        }
    
    def select_optimal_model(self, audio_duration: float, quality_preference: str) -> str:
        """Select optimal model based on duration and quality preference"""
        
        # For very short audio, use base model for speed
        if audio_duration < 30:
            return 'base'
        
        # For long audio, consider memory constraints
        if audio_duration > 3600:  # 1 hour
            return 'small' if quality_preference == 'balanced' else 'base'
        
        # Default selection based on quality preference
        quality_map = {
            'speed': 'base',
            'balanced': 'small',
            'accuracy': 'medium'
        }
        
        return quality_map.get(quality_preference, 'small')
```

### GPU Optimization
```python
# GPU memory management and optimization
import torch
from torch.cuda import synchronize, empty_cache

class GPUOptimizer:
    def __init__(self):
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        self.compute_type = "float16" if self.device == "cuda" else "int8"
        
    def optimize_for_gpu(self):
        """Optimize PyTorch for GPU usage"""
        if torch.cuda.is_available():
            # Enable memory efficient attention
            torch.backends.cuda.enable_flash_sdp(True)
            
            # Set memory fraction to avoid OOM
            torch.cuda.set_per_process_memory_fraction(0.8)
            
            # Enable tensor cores for faster computation
            torch.backends.cudnn.allow_tf32 = True
            torch.backends.cuda.matmul.allow_tf32 = True
    
    def load_model_optimized(self, model_name: str):
        """Load model with GPU optimizations"""
        import whisperx
        
        # Load with optimal compute type
        model = whisperx.load_model(
            model_name,
            device=self.device,
            compute_type=self.compute_type,
            asr_options={"suppress_numerals": True, "max_new_tokens": None}
        )
        
        # Enable mixed precision if available
        if self.device == "cuda" and hasattr(model, 'half'):
            model = model.half()
        
        return model
    
    def cleanup_gpu_memory(self):
        """Clean up GPU memory after processing"""
        if torch.cuda.is_available():
            empty_cache()
            synchronize()
```

### Audio Preprocessing Optimization
```python
# Optimized audio preprocessing
import librosa
import numpy as np
from scipy import signal

class AudioPreprocessor:
    def __init__(self):
        self.target_sr = 16000
        self.chunk_length = 30  # seconds
        
    def preprocess_audio_optimized(self, audio_path: str) -> np.ndarray:
        """Optimized audio preprocessing pipeline"""
        
        # Load audio with librosa (faster than whisper's internal loading)
        audio, sr = librosa.load(audio_path, sr=None)
        
        # Resample if necessary (using high-quality resampling)
        if sr != self.target_sr:
            audio = librosa.resample(
                audio, 
                orig_sr=sr, 
                target_sr=self.target_sr,
                res_type='kaiser_best'
            )
        
        # Normalize audio
        audio = self._normalize_audio(audio)
        
        # Remove silence
        audio = self._remove_silence(audio)
        
        return audio
    
    def _normalize_audio(self, audio: np.ndarray) -> np.ndarray:
        """Normalize audio to optimal range"""
        # Remove DC offset
        audio = audio - np.mean(audio)
        
        # Normalize to [-1, 1] range
        max_val = np.max(np.abs(audio))
        if max_val > 0:
            audio = audio / max_val * 0.95
        
        return audio
    
    def _remove_silence(self, audio: np.ndarray, threshold: float = 0.01) -> np.ndarray:
        """Remove silence from audio"""
        # Simple energy-based silence removal
        frame_length = int(0.025 * self.target_sr)  # 25ms frames
        hop_length = int(0.010 * self.target_sr)    # 10ms hop
        
        # Calculate energy
        energy = np.array([
            np.sum(np.abs(audio[i:i+frame_length]**2))
            for i in range(0, len(audio)-frame_length, hop_length)
        ])
        
        # Find non-silent frames
        non_silent = energy > threshold * np.max(energy)
        
        if not np.any(non_silent):
            return audio
        
        # Expand selection to include context
        non_silent = np.convolve(non_silent, np.ones(5), mode='same') > 0
        
        # Extract non-silent audio
        start_frame = np.argmax(non_silent) * hop_length
        end_frame = (len(non_silent) - np.argmax(non_silent[::-1]) - 1) * hop_length
        
        return audio[start_frame:end_frame + frame_length]
```

### Batch Processing Optimization
```python
# Optimized batch processing for multiple files
class BatchTranscriber:
    def __init__(self, model_manager: WhisperModelManager):
        self.model_manager = model_manager
        self.max_batch_size = 8
        
    async def transcribe_batch(self, audio_files: List[str], model_name: str) -> List[Dict]:
        """Process multiple audio files in optimized batches"""
        
        # Group files by similar duration for better batching
        files_with_duration = []
        for file_path in audio_files:
            duration = self._get_audio_duration(file_path)
            files_with_duration.append((file_path, duration))
        
        # Sort by duration
        files_with_duration.sort(key=lambda x: x[1])
        
        # Process in batches
        results = []
        model = await self.model_manager.get_model(model_name)
        
        for i in range(0, len(files_with_duration), self.max_batch_size):
            batch = files_with_duration[i:i + self.max_batch_size]
            batch_results = await self._process_batch(batch, model)
            results.extend(batch_results)
        
        return results
    
    async def _process_batch(self, batch: List[Tuple[str, float]], model) -> List[Dict]:
        """Process a single batch of audio files"""
        tasks = []
        
        for file_path, duration in batch:
            task = asyncio.create_task(
                self._transcribe_single_file(file_path, model)
            )
            tasks.append(task)
        
        return await asyncio.gather(*tasks, return_exceptions=True)
```

## 📊 Monitoring and Profiling

### Application Performance Monitoring
```java
// Java application metrics
@Component
public class PerformanceMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Timer transcriptionTimer;
    private final Counter transcriptionCounter;
    
    public PerformanceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.transcriptionTimer = Timer.builder("transcription.duration")
            .description("Transcription processing time")
            .tag("service", "api")
            .register(meterRegistry);
        this.transcriptionCounter = Counter.builder("transcription.requests")
            .description("Total transcription requests")
            .register(meterRegistry);
    }
    
    public void recordTranscriptionTime(Duration duration, String model, String status) {
        transcriptionTimer.record(duration,
            Tags.of(
                Tag.of("model", model),
                Tag.of("status", status)
            ));
    }
    
    public void incrementTranscriptionRequests(String model) {
        transcriptionCounter.increment(Tags.of(Tag.of("model", model)));
    }
}
```

### Python Performance Profiling
```python
# Python performance monitoring
import time
import psutil
import logging
from functools import wraps
from prometheus_client import Counter, Histogram, Gauge

# Metrics
REQUEST_COUNT = Counter('transcription_requests_total', 'Total transcription requests', ['model', 'status'])
REQUEST_DURATION = Histogram('transcription_duration_seconds', 'Transcription duration', ['model'])
MEMORY_USAGE = Gauge('memory_usage_bytes', 'Current memory usage')
GPU_UTILIZATION = Gauge('gpu_utilization_percent', 'GPU utilization percentage')

def performance_monitor(func):
    """Decorator for performance monitoring"""
    @wraps(func)
    async def wrapper(*args, **kwargs):
        start_time = time.time()
        model_name = kwargs.get('model_name', 'unknown')
        
        try:
            result = await func(*args, **kwargs)
            
            # Record success metrics
            duration = time.time() - start_time
            REQUEST_DURATION.labels(model=model_name).observe(duration)
            REQUEST_COUNT.labels(model=model_name, status='success').inc()
            
            return result
            
        except Exception as e:
            # Record failure metrics
            REQUEST_COUNT.labels(model=model_name, status='error').inc()
            raise e
        
        finally:
            # Update system metrics
            MEMORY_USAGE.set(psutil.virtual_memory().used)
            
            # Update GPU metrics if available
            try:
                import GPUtil
                gpus = GPUtil.getGPUs()
                if gpus:
                    GPU_UTILIZATION.set(gpus[0].load * 100)
            except ImportError:
                pass
    
    return wrapper
```

### Custom Performance Dashboard
```python
# Performance dashboard data collection
class PerformanceDashboard:
    def __init__(self):
        self.metrics_collector = MetricsCollector()
        
    async def get_dashboard_data(self) -> Dict:
        """Collect performance data for dashboard"""
        return {
            'system_metrics': await self._get_system_metrics(),
            'application_metrics': await self._get_application_metrics(),
            'database_metrics': await self._get_database_metrics(),
            'transcription_metrics': await self._get_transcription_metrics()
        }
    
    async def _get_system_metrics(self) -> Dict:
        """Get system-level metrics"""
        return {
            'cpu_usage': psutil.cpu_percent(interval=1),
            'memory_usage': psutil.virtual_memory()._asdict(),
            'disk_usage': psutil.disk_usage('/')._asdict(),
            'network_io': psutil.net_io_counters()._asdict()
        }
    
    async def _get_transcription_metrics(self) -> Dict:
        """Get transcription-specific metrics"""
        # Query from database or metrics store
        return {
            'avg_processing_time': await self._get_avg_processing_time(),
            'success_rate': await self._get_success_rate(),
            'queue_size': await self._get_queue_size(),
            'model_usage': await self._get_model_usage_stats()
        }
```

## 📈 Scaling Strategies

### Horizontal Scaling

#### API Service Scaling
```yaml
# docker-compose.yml for horizontal scaling
version: '3.8'
services:
  api-service:
    image: speechtotext/api-service:latest
    deploy:
      replicas: 3
      resources:
        limits:
          memory: 2G
          cpus: '1.0'
        reservations:
          memory: 1G
          cpus: '0.5'
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    networks:
      - app-network
```

#### Kubernetes Scaling
```yaml
# k8s-deployment.yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: api-service
  template:
    metadata:
      labels:
        app: api-service
    spec:
      containers:
      - name: api-service
        image: speechtotext/api-service:latest
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
---
apiVersion: v1
kind: Service
metadata:
  name: api-service
spec:
  selector:
    app: api-service
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: api-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: api-service
  minReplicas: 3
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

### Auto-scaling Configuration
```python
# Auto-scaling based on queue size
class AutoScaler:
    def __init__(self):
        self.min_replicas = 2
        self.max_replicas = 20
        self.target_queue_size = 10
        
    async def check_scaling_needs(self):
        """Check if scaling is needed based on metrics"""
        current_queue_size = await self._get_queue_size()
        current_replicas = await self._get_current_replicas()
        
        if current_queue_size > self.target_queue_size * 2:
            # Scale up
            new_replicas = min(current_replicas + 2, self.max_replicas)
            await self._scale_to(new_replicas)
            
        elif current_queue_size < self.target_queue_size / 2:
            # Scale down
            new_replicas = max(current_replicas - 1, self.min_replicas)
            await self._scale_to(new_replicas)
    
    async def _scale_to(self, replicas: int):
        """Scale service to specified number of replicas"""
        # Implementation depends on orchestration platform
        # Docker Swarm, Kubernetes, etc.
        pass
```

## 🧪 Performance Testing

### Load Testing Scripts
```python
# load_test.py - Locust load testing
from locust import HttpUser, task, between
import random
import string

class TranscriptionUser(HttpUser):
    wait_time = between(1, 5)
    
    def on_start(self):
        """Setup test user"""
        self.api_key = "test-api-key"
        self.client.headers.update({
            "Authorization": f"Bearer {self.api_key}"
        })
    
    @task(3)
    def upload_short_audio(self):
        """Test short audio upload (< 1 minute)"""
        files = {'file': ('test-short.wav', self._generate_audio_data(30))}
        data = {
            'language': 'en',
            'model': 'base',
            'sync': 'true'
        }
        
        with self.client.post('/api/v1/transcriptions', files=files, data=data, catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Failed with status {response.status_code}")
    
    @task(1) 
    def upload_long_audio(self):
        """Test long audio upload (> 5 minutes)"""
        files = {'file': ('test-long.wav', self._generate_audio_data(300))}
        data = {
            'language': 'en',
            'model': 'base', 
            'sync': 'false'
        }
        
        response = self.client.post('/api/v1/transcriptions', files=files, data=data)
        if response.status_code == 202:
            job_id = response.json()['id']
            self._poll_for_completion(job_id)
    
    @task(2)
    def list_transcriptions(self):
        """Test listing transcriptions"""
        self.client.get('/api/v1/transcriptions?limit=10')
    
    @task(1)
    def health_check(self):
        """Test health check endpoint"""
        self.client.get('/api/v1/health')
    
    def _generate_audio_data(self, duration_seconds: int) -> bytes:
        """Generate dummy audio data for testing"""
        # Simple sine wave generation
        import numpy as np
        import wave
        import io
        
        sample_rate = 16000
        samples = duration_seconds * sample_rate
        
        # Generate sine wave
        frequency = 440  # A4 note
        audio_data = np.sin(2 * np.pi * frequency * np.linspace(0, duration_seconds, samples))
        audio_data = (audio_data * 32767).astype(np.int16)
        
        # Create WAV file in memory
        wav_buffer = io.BytesIO()
        with wave.open(wav_buffer, 'wb') as wav_file:
            wav_file.setnchannels(1)  # Mono
            wav_file.setsampwidth(2)  # 16-bit
            wav_file.setframerate(sample_rate)
            wav_file.writeframes(audio_data.tobytes())
        
        return wav_buffer.getvalue()
    
    def _poll_for_completion(self, job_id: str):
        """Poll for job completion"""
        max_attempts = 30
        for _ in range(max_attempts):
            response = self.client.get(f'/api/v1/transcriptions/{job_id}')
            if response.status_code == 200:
                result = response.json()
                if result['status'] in ['COMPLETED', 'FAILED']:
                    break
            time.sleep(2)
```

### Benchmarking Tools
```bash
#!/bin/bash
# benchmark.sh - Performance benchmarking

echo "=== Performance Benchmark ==="

# API endpoint benchmarking with Apache Bench
echo "Running API benchmark..."
ab -n 1000 -c 10 -H "Authorization: Bearer test-api-key" \
   https://api.yourdomain.com/api/v1/health

# Database query benchmarking
echo "Running database benchmark..."
docker exec -it postgres-container pgbench \
  -c 10 -j 2 -T 60 \
  -h localhost -U speechtotext speechtotext

# File upload benchmarking
echo "Running upload benchmark..."
for i in {1..10}; do
  time curl -X POST "https://api.yourdomain.com/api/v1/transcriptions" \
    -H "Authorization: Bearer test-api-key" \
    -F "file=@test-audio.wav" \
    -F "language=en" \
    -F "sync=true"
done

echo "=== Benchmark completed ==="
```

### Performance Regression Testing
```python
# performance_regression_test.py
import time
import statistics
import requests
from typing import List, Dict

class PerformanceRegressionTest:
    def __init__(self, base_url: str, api_key: str):
        self.base_url = base_url
        self.api_key = api_key
        self.session = requests.Session()
        self.session.headers.update({
            "Authorization": f"Bearer {api_key}"
        })
    
    def run_regression_tests(self) -> Dict:
        """Run performance regression tests"""
        results = {
            'api_response_time': self._test_api_response_time(),
            'upload_performance': self._test_upload_performance(),
            'concurrent_requests': self._test_concurrent_requests()
        }
        return results
    
    def _test_api_response_time(self) -> Dict:
        """Test API response time regression"""
        response_times = []
        
        for _ in range(100):
            start_time = time.time()
            response = self.session.get(f"{self.base_url}/api/v1/health")
            end_time = time.time()
            
            if response.status_code == 200:
                response_times.append(end_time - start_time)
        
        return {
            'mean': statistics.mean(response_times),
            'median': statistics.median(response_times),
            'p95': statistics.quantiles(response_times, n=20)[18],  # 95th percentile
            'p99': statistics.quantiles(response_times, n=100)[98]  # 99th percentile
        }
    
    def _test_upload_performance(self) -> Dict:
        """Test file upload performance"""
        # Test with different file sizes
        file_sizes = [1, 5, 10, 50]  # MB
        results = {}
        
        for size_mb in file_sizes:
            upload_times = []
            
            for _ in range(5):  # 5 iterations per size
                test_file = self._generate_test_file(size_mb)
                
                start_time = time.time()
                response = self.session.post(
                    f"{self.base_url}/api/v1/transcriptions",
                    files={'file': ('test.wav', test_file)},
                    data={'language': 'en', 'sync': 'true'}
                )
                end_time = time.time()
                
                if response.status_code == 200:
                    upload_times.append(end_time - start_time)
            
            results[f"{size_mb}MB"] = {
                'mean': statistics.mean(upload_times),
                'max': max(upload_times),
                'min': min(upload_times)
            }
        
        return results
```

---

This comprehensive performance tuning guide provides detailed optimization strategies for all components of the Speech to Text service. Regular performance monitoring and optimization should be an ongoing process to maintain optimal system performance as load increases.

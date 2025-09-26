# Speech to Text Service

A production-ready backend system that converts audio files to text using WhisperX for transcription. The solution is split into two microservices within a single mono-repository.

## 🎯 Implementation Status

**Current Milestone: M3 - Transcription Service** ✅

**Completed Milestones:**
- ✅ M1: Project Skeleton - Gradle multi-project, Docker Compose infrastructure  
- ✅ M2: Core Spring Boot API with REST endpoints, database entities, and business logic
- ✅ M3: WhisperX transcription service with alignment and diarization

**Upcoming Milestones:**
- [x] M1 - Project skeleton
- [x] M2 - Core Spring Boot API  
- [x] M3 - Transcription Service
- [x] M4 - Integration Flow
- [x] M5 - Async & Sync Behavior
- [ ] M6 - Tests & CI/CD
- [ ] M7 - Production Documentation

## 🏗️ Architecture

```
┌─────────────┐    ┌──────────────────┐    ┌─────────────────────┐
│   Client    │───▶│   API Service    │───▶│ Transcription       │
│             │    │  (Spring Boot)   │    │ Service (FastAPI)   │
└─────────────┘    └──────────────────┘    └─────────────────────┘
                           │                         │
                           ▼                         ▼
                   ┌──────────────┐          ┌─────────────┐
                   │  PostgreSQL  │          │ MinIO (S3)  │
                   │   Database   │          │   Storage   │
                   └──────────────┘          └─────────────┘
```

## 🚀 Quick Start

### Prerequisites

- Docker & Docker Compose
- Java 21+ (for local development)
- Python 3.11+ (for local development)

### Run with Docker Compose

1. **Clone and navigate to the project**:
   ```bash
   git clone <repository-url>
   cd SpeechToText
   ```

2. **Start all services**:
   ```bash
   cd infra
   docker-compose up --build
   ```

3. **Verify services are running**:
   - API Service: http://localhost:8080/actuator/health
   - Transcription Service: http://localhost:8081/
   - MinIO Console: http://localhost:9001 (minioadmin/minioadmin)

### Test the API

Upload an audio file for transcription:

```bash
# Async transcription (default for large files)
curl -X POST "http://localhost:8080/api/v1/transcriptions" \
  -F "file=@sample.wav" \
  -F "language=en" \
  -F "sync=false"

# Response: {"jobId": "uuid", "status": "PENDING"}

# Check transcription status
curl "http://localhost:8080/api/v1/transcriptions/{jobId}"

# Download transcript
curl "http://localhost:8080/api/v1/transcriptions/{jobId}/download"
```

## 📝 API Examples

### Upload Audio for Transcription
```bash
curl -X POST "http://localhost:8080/api/v1/transcriptions" \
  -F "file=@/path/to/audio.wav" \
  -F "language=en" \
  -F "model=base" \
  -F "sync=false"
```

**Response (Async):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "message": "Transcription job created successfully",
  "status_url": "/api/v1/transcriptions/550e8400-e29b-41d4-a716-446655440000"
}
```

### Check Job Status
```bash
curl -X GET "http://localhost:8080/api/v1/transcriptions/550e8400-e29b-41d4-a716-446655440000"
```

**Response (Completed):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "original_filename": "audio.wav",
  "status": "COMPLETED",
  "transcript_text": "Hello world, this is a test recording.",
  "model": "base",
  "language": "en",
  "file_size_bytes": 1024000,
  "duration_seconds": 45.67,
  "created_at": "2023-12-01T10:00:00",
  "updated_at": "2023-12-01T10:05:30",
  "download_url": "/api/v1/transcriptions/550e8400-e29b-41d4-a716-446655440000/download"
}
```

### Download Transcript
```bash
curl -X GET "http://localhost:8080/api/v1/transcriptions/550e8400-e29b-41d4-a716-446655440000/download" \
  -o transcript.txt
```

## 📁 Project Structure

```
speech-to-text/
├── services/
│   ├── api-service/          # Spring Boot REST API
│   │   ├── src/main/java/    # Application source code
│   │   ├── src/main/resources/ # Configuration files
│   │   ├── src/test/         # Unit and integration tests
│   │   ├── build.gradle      # Gradle build configuration
│   │   └── Dockerfile        # Container configuration
│   └── transcription-service/ # Python FastAPI service
│       ├── app/              # Application source code
│       ├── tests/            # Python tests
│       ├── requirements.txt  # Python dependencies
│       └── Dockerfile        # Container configuration
├── infra/
│   ├── docker-compose.yml    # Local development infrastructure
│   └── k8s/                  # Kubernetes manifests (optional)
├── docs/                     # Documentation and API specs
├── build.gradle             # Root Gradle configuration
├── settings.gradle          # Multi-project settings
└── README.md                # This file
```

## 🛠️ Services

### API Service (Spring Boot)

**Endpoints:**
- `POST /api/v1/transcriptions` - Upload audio file for transcription
- `GET /api/v1/transcriptions/{id}` - Get transcription status and result
- `GET /api/v1/transcriptions/{id}/download` - Download transcript as text file
- `GET /api/v1/actuator/health` - Health check

**Features:**
- Multi-part file upload with validation
- Async/sync processing modes
- PostgreSQL persistence with Flyway migrations
- S3-compatible storage (MinIO)
- OpenAPI documentation
- Prometheus metrics
- Rate limiting

### Transcription Service (FastAPI/Python)

**Endpoints:**
- `POST /transcribe` - Process audio transcription
- `GET /health` - Health check
- `GET /metrics` - Prometheus metrics

**Features:**
- WhisperX integration for high-quality transcription
- Speaker diarization support
- GPU acceleration (when available)
- S3 storage integration
- Webhook callbacks for async processing

## ⚙️ Configuration

### Environment Variables

#### API Service
```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/speechtotext
DATABASE_USERNAME=speechtotext
DATABASE_PASSWORD=speechtotext123
S3_ENDPOINT=http://localhost:9000
S3_ACCESS_KEY=minioadmin
S3_SECRET_KEY=minioadmin
S3_BUCKET_NAME=speechtotext
TRANSCRIPTION_SERVICE_URL=http://localhost:8081
```

#### Transcription Service
```bash
PORT=8081
S3_ENDPOINT=http://localhost:9000
S3_ACCESS_KEY=minioadmin
S3_SECRET_KEY=minioadmin
S3_BUCKET_NAME=speechtotext
HUGGINGFACE_TOKEN=your_token_here  # Required for diarization
```

### Spring Profiles

- `dev` - Local development with debug logging
- `docker` - Docker container deployment
- `prod` - Production deployment
- `test` - Test environment with H2 database

## 📊 Monitoring

Access the monitoring dashboards:
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)

Enable monitoring services:
```bash
docker-compose -f infra/docker-compose.yml --profile monitoring up
```

## 🧪 Testing

### Run Tests

```bash
# Java tests
./gradlew test

# Python tests (requires Python environment)
cd services/transcription-service
pip install -r requirements.txt
pytest tests/
```

### Integration Testing

The project includes comprehensive integration tests using Testcontainers for realistic testing with actual databases.

## 🚀 Development

### Local Development Setup

1. **Database Setup**:
   ```bash
   docker-compose -f infra/docker-compose.yml up postgres minio -d
   ```

2. **Run API Service**:
   ```bash
   ./gradlew :services:api-service:bootRun
   ```

3. **Run Transcription Service**:
   ```bash
   cd services/transcription-service
   pip install -r requirements.txt
   python -m uvicorn app.main:app --reload --port 8081
   ```

### Code Style

- **Java**: Google Java Format with Spotless
- **Python**: Black formatter with isort

Format code:
```bash
./gradlew spotlessApply  # Java
black . && isort .       # Python
```

## 🐳 Docker & Kubernetes

### GPU Support

For GPU-accelerated transcription, uncomment GPU sections in docker-compose.yml:

```yaml
# Uncomment in transcription-service:
environment:
  CUDA_VISIBLE_DEVICES: 0
deploy:
  resources:
    reservations:
      devices:
        - driver: nvidia
          count: 1
          capabilities: [gpu]
```

### Production Deployment

See `/infra/k8s/` for Kubernetes deployment manifests (optional).

## 📋 API Documentation

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/api-docs

## 🔒 Security

- File type validation (wav, mp3, m4a, flac)
- File size limits (configurable, default 100MB)
- Rate limiting for uploads
- Input validation and sanitization
- Internal endpoint protection

## 📈 Performance

### Recommended Hardware

- **CPU**: 8+ cores for CPU-based transcription
- **GPU**: NVIDIA GPU with 8GB+ VRAM for optimal performance
- **RAM**: 16GB+ system memory
- **Storage**: SSD for temporary audio file processing

### Configuration Tuning

- Use `compute_type=int8` for lower memory usage
- Adjust batch sizes based on available memory
- Configure appropriate worker counts for uvicorn

## 🤝 Contributing

1. Follow conventional commit format: `feat:`, `fix:`, `chore:`, `test:`, `docs:`
2. Run tests before submitting PR
3. Ensure code formatting is applied
4. Update documentation for new features

## 📄 License

[Your License Here]

---

## 🎯 Current Status (M1 Completed)

✅ **M1 - Project skeleton setup**
- ✅ Gradle multi-project structure
- ✅ Docker Compose with PostgreSQL, MinIO
- ✅ Basic Spring Boot application structure
- ✅ FastAPI transcription service skeleton
- ✅ Comprehensive README documentation
- ✅ Health check endpoints

**Next Steps**: M2 - Implement core Spring Boot API with persistence layer

### M2 - Core Spring Boot API ✅

**Implemented Features:**
- ✅ REST API endpoints:
  - `POST /api/v1/transcriptions` - Upload audio files for transcription
  - `GET /api/v1/transcriptions/{id}` - Get job status and results  
  - `GET /api/v1/transcriptions/{id}/download` - Download transcript as text file
  - `POST /internal/v1/transcriptions/{id}/callback` - Internal callback endpoint

- ✅ Database layer with Flyway migrations:
  - `jobs` table with comprehensive job tracking
  - PostgreSQL integration with proper indexing
  - Automatic timestamp management

- ✅ Domain models and DTOs:
  - `JobEntity` with full lifecycle status tracking
  - Request/Response DTOs with validation
  - MapStruct mapping between entities and DTOs

- ✅ S3/MinIO integration:
  - File upload to object storage
  - Presigned URL generation for downloads
  - Proper error handling and bucket management

- ✅ Service layer with business logic:
  - File validation (size, MIME type)
  - Job lifecycle management
  - Error handling and logging

- ✅ Configuration management:
  - Environment-specific profiles (dev, prod)
  - Externalized configuration with environment variables
  - OpenAPI/Swagger documentation setup

**Technical Highlights:**
- Java 21 with Spring Boot 3.5.6
- Clean architecture with proper separation of concerns
- Comprehensive error handling and validation
- Structured logging with correlation IDs
- Health checks and metrics endpoints (Actuator)

### M3 - WhisperX Transcription Service ✅

**Implemented Features:**
- ✅ **FastAPI Application**:
  - Async/sync transcription endpoints with comprehensive request/response models
  - Health checks and service readiness validation
  - Prometheus metrics integration
  - Production-ready error handling and logging

- ✅ **WhisperX Integration**:
  - Multi-model support (tiny, base, small, medium, large)
  - Automatic language detection and manual specification
  - Word-level timestamp alignment for precise timing
  - Optional speaker diarization with HuggingFace integration
  - GPU acceleration support with fallback to CPU

- ✅ **Advanced Processing Pipeline**:
  - Configurable compute types (float16, float32, int8) for memory optimization
  - Batch processing with adjustable batch sizes
  - Model caching for improved performance
  - Background task processing for async operations

- ✅ **Storage Integration**:
  - S3/MinIO download and upload of audio files and results
  - Automatic transcript and timestamp file generation
  - Temporary file cleanup and resource management
  - Presigned URL support for secure file access

- ✅ **Callback System**:
  - HTTP webhook callbacks to API service with retry logic
  - Exponential backoff for failed callback attempts
  - Comprehensive callback payload with all processing metadata
  - Idempotent callback handling

- ✅ **Production Features**:
  - Multi-stage Docker builds (CPU/GPU variants)
  - Comprehensive test suite with mocking
  - Environment-based configuration
  - Resource cleanup and memory management
  - Structured logging with correlation IDs

**Technical Highlights:**
- **Performance**: GPU acceleration, model caching, efficient batch processing
- **Reliability**: Retry mechanisms, error handling, resource cleanup
- **Scalability**: Stateless design, horizontal scaling support
- **Observability**: Health checks, metrics, structured logging
- **Security**: Non-root containers, input validation, secure file handling

**API Endpoints:**
- `POST /transcribe` - Process audio transcription with WhisperX
- `GET /health` - Service health and readiness check
- `GET /models` - List available Whisper models
- `GET /metrics` - Prometheus metrics endpoint

### M4 - Integration Flow ✅

**Service-to-Service Communication:**
- **TranscriptionServiceClient**: HTTP client for calling transcription service endpoints with proper error handling and timeouts
- **RestTemplate Configuration**: Configured with connection pooling, timeouts (300s), and proper JSON serialization
- **Callback Mechanism**: Internal endpoint `/internal/v1/transcriptions/{id}/callback` for receiving transcription results

**API Integration Flow:**
1. **File Upload & Job Creation**: Spring API receives file → validates → uploads to S3 → creates job (PENDING)
2. **Service Invocation**: API calls transcription service `/transcribe` with job_id, s3_url, and callback_url
3. **Job Status Update**: Job status changes to PROCESSING when successfully submitted to transcription service
4. **Async Processing**: Transcription service processes audio in background using WhisperX
5. **Result Callback**: Transcription service calls back API with results (COMPLETED/FAILED status)
6. **Database Update**: API updates job with transcript text, timestamps JSON, and metadata

**Enhanced Error Handling:**
- **Service Communication Failures**: Jobs marked as FAILED if transcription service is unreachable
- **Callback Validation**: Comprehensive validation of callback payloads with proper error responses
- **Status Transitions**: Proper job status lifecycle management (PENDING → PROCESSING → COMPLETED/FAILED)
- **Timeout Handling**: Configurable timeouts for transcription service calls

**Configuration:**
- Service URLs configurable via environment variables (`TRANSCRIPTION_SERVICE_URL`, `CALLBACK_BASE_URL`)
- HTTP client timeouts and connection pooling properly configured
- Docker Compose networking enables service-to-service communication

**Data Exchange:**
- **Request Format**: JSON with job_id, s3_url, callback_url, and processing options (diarization, alignment)
- **Callback Format**: Comprehensive callback with status, transcript_text, detailed segments, speaker information, and metadata
- **Timestamp Storage**: Detailed transcription results stored as JSON including word-level alignments and speaker segments

### M5 - Async & Sync Behavior ✅

**Intelligent Processing Mode Selection:**
- **Automatic Decision Logic**: Service automatically chooses sync/async based on file size and estimated duration
- **User Override**: Explicit `sync=true/false` parameter overrides automatic decision
- **Size-Based Heuristics**: Files < 1MB automatically processed synchronously
- **Duration Estimation**: Rough audio duration estimation based on file size (1MB ≈ 60 seconds)
- **Threshold Configuration**: Configurable sync threshold (default: 60 seconds)

**Synchronous Processing:**
- **Direct Response**: Returns complete transcription result immediately (HTTP 200)
- **Timeout Protection**: Configurable timeout (default: 120 seconds) prevents hanging requests
- **Error Fallback**: Failed sync requests properly marked as FAILED with detailed error messages
- **Client Experience**: Immediate results for short audio files without polling

**Asynchronous Processing:**
- **Background Processing**: Long files processed in background with callback notifications
- **Job Tracking**: Returns job ID and status URL for progress monitoring (HTTP 202)
- **Webhook Callbacks**: Transcription service notifies API service upon completion
- **Scalability**: Non-blocking for large files that take minutes to process

**Enhanced API Responses:**
- **Content Negotiation**: Different response types based on processing mode
- **HTTP Status Codes**: 200 for completed sync, 202 for accepted async jobs
- **Response Format**: `TranscriptionResponse` for sync, `TranscriptionJobResponse` for async
- **Error Handling**: Proper error responses with detailed messages and error codes

**Configuration Management:**
- **Environment Variables**: `SYNC_THRESHOLD` and `SYNC_TIMEOUT` for fine-tuning
- **Profile Support**: Different thresholds for dev/prod environments
- **Service Communication**: Enhanced client with both sync and async submission methods

**Processing Flow Examples:**
- **Small File (< 1MB)**: Upload → Process → Return transcript immediately
- **Large File (> threshold)**: Upload → Create job → Background processing → Callback notification
- **Explicit Sync**: Any file size can be forced to sync mode (with timeout protection)
- **Explicit Async**: Even small files can be processed asynchronously if requested
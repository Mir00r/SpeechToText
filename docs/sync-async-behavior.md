# Sync vs Async Behavior Examples

## Sync Processing (for short audio files)

### Small File (< 1MB or estimated < 60 seconds)
```bash
# Automatic sync processing for small files
curl -X POST "http://localhost:8080/api/v1/transcriptions" \
  -F "file=@short_audio.wav" \
  -F "language=en" \
  -F "model=base"

# Response (HTTP 200 - immediate result):
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "original_filename": "short_audio.wav",
  "status": "COMPLETED",
  "transcript_text": "Hello, this is a short recording.",
  "model": "base",
  "language": "en",
  "file_size_bytes": 512000,
  "duration_seconds": 30.5,
  "created_at": "2025-09-26T10:00:00",
  "updated_at": "2025-09-26T10:00:15",
  "download_url": "/api/v1/transcriptions/550e8400-e29b-41d4-a716-446655440000/download"
}
```

### Force Sync Processing
```bash
# Explicitly request sync processing (even for larger files)
curl -X POST "http://localhost:8080/api/v1/transcriptions" \
  -F "file=@medium_audio.wav" \
  -F "language=en" \
  -F "sync=true" \
  -F "model=base"

# Response (HTTP 200 - waits up to 120 seconds for completion)
```

## Async Processing (for long audio files)

### Large File (> sync threshold)
```bash
# Automatic async processing for large files
curl -X POST "http://localhost:8080/api/v1/transcriptions" \
  -F "file=@long_audio.wav" \
  -F "language=en" \
  -F "model=base"

# Response (HTTP 202 - job created):
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "status": "PROCESSING",
  "message": "Transcription job created successfully",
  "status_url": "/api/v1/transcriptions/550e8400-e29b-41d4-a716-446655440001"
}
```

### Force Async Processing
```bash
# Explicitly request async processing (even for small files)
curl -X POST "http://localhost:8080/api/v1/transcriptions" \
  -F "file=@short_audio.wav" \
  -F "language=en" \
  -F "sync=false" \
  -F "model=base"

# Response (HTTP 202 - job created for background processing)
```

## Decision Logic

The service automatically decides between sync/async based on:

1. **Explicit User Preference**: If `sync=true` or `sync=false` is specified, honor that choice
2. **File Size Estimation**: Files < 1MB are processed synchronously
3. **Duration Threshold**: Files estimated to be < 60 seconds are processed synchronously
4. **Timeout Protection**: Sync processing has a 120-second timeout

## Configuration

```yaml
app:
  transcription:
    sync-threshold-seconds: 60    # Files estimated < 60s processed sync
    sync-timeout-seconds: 120     # Max wait time for sync processing
```

## Error Handling

### Sync Processing Timeout
```json
{
  "error_code": "SYNC_TIMEOUT",
  "message": "Synchronous transcription timed out after 120 seconds",
  "details": "Large file or slow processing - try async mode",
  "timestamp": "2025-09-26T10:05:00"
}
```

### Sync Processing Failure
```json
{
  "error_code": "SYNC_FAILED",
  "message": "Synchronous transcription failed: Service unavailable",
  "details": "Transcription service error - job will be marked as failed",
  "timestamp": "2025-09-26T10:05:00"
}
```

# API Documentation

Comprehensive API documentation for the Speech to Text service, including endpoints, request/response formats, authentication, and integration examples.

## 📋 Table of Contents

- [API Overview](#api-overview)
- [Authentication](#authentication)
- [Endpoints](#endpoints)
- [Request/Response Formats](#requestresponse-formats)
- [Error Handling](#error-handling)
- [Rate Limiting](#rate-limiting)
- [WebhooksCallbacks](#webhookscallbacks)
- [SDKs and Integration](#sdks-and-integration)
- [Examples](#examples)

## 🌐 API Overview

### Base URLs
- **Production**: `https://api.yourdomain.com`
- **Staging**: `https://staging-api.yourdomain.com`
- **Development**: `http://localhost:8080`

### API Version
Current version: `v1`

All endpoints are prefixed with `/api/v1/`

### Content Types
- Request: `application/json`, `multipart/form-data`
- Response: `application/json`

### HTTP Methods
- `GET` - Retrieve resources
- `POST` - Create resources
- `DELETE` - Delete resources

## 🔐 Authentication

### API Key Authentication

All requests require an API key in the header:

```http
Authorization: Bearer YOUR_API_KEY
```

### Rate Limiting Headers

```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1640995200
```

## 📡 Endpoints

### 1. Upload Audio for Transcription

Upload an audio file for transcription processing.

```http
POST /api/v1/transcriptions
```

#### Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | File | ✅ | Audio file (wav, mp3, m4a, flac) |
| `language` | String | ✅ | Language code (en, es, fr, etc.) |
| `model` | String | ❌ | Whisper model (tiny, base, small, medium, large) |
| `sync` | Boolean | ❌ | Process synchronously (default: auto-detect) |
| `diarization` | Boolean | ❌ | Enable speaker diarization (default: false) |
| `alignment` | Boolean | ❌ | Enable word-level alignment (default: true) |

#### Request Example

```bash
curl -X POST "https://api.yourdomain.com/api/v1/transcriptions" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -F "file=@audio.wav" \
  -F "language=en" \
  -F "model=base" \
  -F "sync=false" \
  -F "diarization=true"
```

#### Response (Async)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "message": "Transcription job created successfully",
  "created_at": "2023-12-01T10:00:00Z",
  "status_url": "/api/v1/transcriptions/550e8400-e29b-41d4-a716-446655440000",
  "estimated_completion": "2023-12-01T10:05:00Z"
}
```

#### Response (Sync)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "original_filename": "audio.wav",
  "status": "COMPLETED",
  "transcript_text": "Hello world, this is a test recording.",
  "language": "en",
  "model": "base",
  "file_size_bytes": 1024000,
  "duration_seconds": 45.67,
  "created_at": "2023-12-01T10:00:00Z",
  "updated_at": "2023-12-01T10:02:30Z",
  "segments": [
    {
      "start": 0.0,
      "end": 2.5,
      "text": "Hello world",
      "speaker": "SPEAKER_00"
    },
    {
      "start": 2.5,
      "end": 5.0,
      "text": "this is a test recording",
      "speaker": "SPEAKER_00"
    }
  ],
  "speakers": [
    {
      "id": "SPEAKER_00",
      "speaking_time": 45.67
    }
  ],
  "download_url": "/api/v1/transcriptions/550e8400-e29b-41d4-a716-446655440000/download"
}
```

### 2. Get Transcription Status

Retrieve the status and results of a transcription job.

```http
GET /api/v1/transcriptions/{id}
```

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | ✅ | Transcription job ID |

#### Request Example

```bash
curl -X GET "https://api.yourdomain.com/api/v1/transcriptions/550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer YOUR_API_KEY"
```

#### Response (Pending)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "original_filename": "audio.wav",
  "status": "PROCESSING",
  "language": "en",
  "model": "base",
  "file_size_bytes": 1024000,
  "created_at": "2023-12-01T10:00:00Z",
  "updated_at": "2023-12-01T10:01:00Z",
  "progress": 45,
  "estimated_completion": "2023-12-01T10:05:00Z"
}
```

#### Response (Completed)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "original_filename": "audio.wav",
  "status": "COMPLETED",
  "transcript_text": "Hello world, this is a test recording.",
  "language": "en",
  "model": "base",
  "file_size_bytes": 1024000,
  "duration_seconds": 45.67,
  "created_at": "2023-12-01T10:00:00Z",
  "updated_at": "2023-12-01T10:02:30Z",
  "segments": [
    {
      "start": 0.0,
      "end": 2.5,
      "text": "Hello world",
      "confidence": 0.95,
      "speaker": "SPEAKER_00",
      "words": [
        {
          "word": "Hello",
          "start": 0.0,
          "end": 0.5,
          "confidence": 0.98
        },
        {
          "word": "world",
          "start": 0.6,
          "end": 1.2,
          "confidence": 0.92
        }
      ]
    }
  ],
  "speakers": [
    {
      "id": "SPEAKER_00",
      "speaking_time": 45.67,
      "segments_count": 15
    }
  ],
  "processing_metadata": {
    "processing_time_seconds": 23.5,
    "model_load_time_seconds": 2.1,
    "inference_time_seconds": 21.4,
    "gpu_used": true,
    "batch_size": 16
  },
  "download_url": "/api/v1/transcriptions/550e8400-e29b-41d4-a716-446655440000/download"
}
```

#### Response (Failed)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "original_filename": "audio.wav",
  "status": "FAILED",
  "error_message": "Unsupported audio format",
  "error_code": "INVALID_AUDIO_FORMAT",
  "language": "en",
  "model": "base",
  "file_size_bytes": 1024000,
  "created_at": "2023-12-01T10:00:00Z",
  "updated_at": "2023-12-01T10:01:30Z"
}
```

### 3. Download Transcript

Download the transcript as a text file.

```http
GET /api/v1/transcriptions/{id}/download
```

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `format` | String | ❌ | Output format (txt, json, srt, vtt) |

#### Request Example

```bash
curl -X GET "https://api.yourdomain.com/api/v1/transcriptions/550e8400-e29b-41d4-a716-446655440000/download?format=srt" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -o transcript.srt
```

#### Response Headers

```http
Content-Type: text/plain; charset=utf-8
Content-Disposition: attachment; filename="transcript.txt"
```

#### Response Body (TXT format)

```
Hello world, this is a test recording.
```

#### Response Body (SRT format)

```
1
00:00:00,000 --> 00:00:02,500
Hello world

2
00:00:02,500 --> 00:00:05,000
this is a test recording
```

#### Response Body (JSON format)

```json
{
  "transcript_text": "Hello world, this is a test recording.",
  "segments": [
    {
      "start": 0.0,
      "end": 2.5,
      "text": "Hello world",
      "speaker": "SPEAKER_00"
    },
    {
      "start": 2.5,
      "end": 5.0,
      "text": "this is a test recording",
      "speaker": "SPEAKER_00"
    }
  ],
  "speakers": [
    {
      "id": "SPEAKER_00",
      "speaking_time": 45.67
    }
  ]
}
```

### 4. List Transcriptions

List all transcription jobs for the authenticated user.

```http
GET /api/v1/transcriptions
```

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `status` | String | ❌ | Filter by status (pending, processing, completed, failed) |
| `language` | String | ❌ | Filter by language code |
| `page` | Integer | ❌ | Page number (default: 1) |
| `limit` | Integer | ❌ | Items per page (default: 10, max: 100) |
| `sort` | String | ❌ | Sort order (created_at, updated_at, duration) |
| `order` | String | ❌ | Sort direction (asc, desc) |

#### Request Example

```bash
curl -X GET "https://api.yourdomain.com/api/v1/transcriptions?status=completed&limit=5&sort=created_at&order=desc" \
  -H "Authorization: Bearer YOUR_API_KEY"
```

#### Response

```json
{
  "transcriptions": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "original_filename": "audio.wav",
      "status": "COMPLETED",
      "language": "en",
      "model": "base",
      "duration_seconds": 45.67,
      "created_at": "2023-12-01T10:00:00Z",
      "updated_at": "2023-12-01T10:02:30Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 5,
    "total": 25,
    "pages": 5
  }
}
```

### 5. Delete Transcription

Delete a transcription job and associated files.

```http
DELETE /api/v1/transcriptions/{id}
```

#### Request Example

```bash
curl -X DELETE "https://api.yourdomain.com/api/v1/transcriptions/550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer YOUR_API_KEY"
```

#### Response

```json
{
  "message": "Transcription deleted successfully",
  "deleted_at": "2023-12-01T11:00:00Z"
}
```

### 6. Health Check

Check API service health and status.

```http
GET /api/v1/health
```

#### Response

```json
{
  "status": "healthy",
  "timestamp": "2023-12-01T10:00:00Z",
  "version": "1.0.0",
  "services": {
    "database": "healthy",
    "storage": "healthy",
    "transcription_service": "healthy"
  },
  "metrics": {
    "active_transcriptions": 5,
    "queue_size": 2,
    "average_processing_time": 23.5
  }
}
```

### 7. Get Supported Languages

Retrieve list of supported languages for transcription.

```http
GET /api/v1/languages
```

#### Response

```json
{
  "languages": [
    {
      "code": "en",
      "name": "English",
      "native_name": "English",
      "models": ["tiny", "base", "small", "medium", "large"]
    },
    {
      "code": "es", 
      "name": "Spanish",
      "native_name": "Español",
      "models": ["tiny", "base", "small", "medium", "large"]
    },
    {
      "code": "fr",
      "name": "French", 
      "native_name": "Français",
      "models": ["tiny", "base", "small", "medium", "large"]
    }
  ]
}
```

### 8. Get Available Models

Retrieve list of available Whisper models.

```http
GET /api/v1/models
```

#### Response

```json
{
  "models": [
    {
      "name": "tiny",
      "size_mb": 39,
      "speed": "fastest",
      "accuracy": "lowest",
      "memory_gb": 1
    },
    {
      "name": "base",
      "size_mb": 74,
      "speed": "fast",
      "accuracy": "good",
      "memory_gb": 1
    },
    {
      "name": "small",
      "size_mb": 244,
      "speed": "medium",
      "accuracy": "better",
      "memory_gb": 2
    },
    {
      "name": "medium",
      "size_mb": 769,
      "speed": "slow",
      "accuracy": "high",
      "memory_gb": 5
    },
    {
      "name": "large",
      "size_mb": 1550,
      "speed": "slowest",
      "accuracy": "highest",
      "memory_gb": 10
    }
  ]
}
```

## 🚨 Error Handling

### HTTP Status Codes

| Code | Description |
|------|-------------|
| `200` | Success |
| `202` | Accepted (async processing) |
| `400` | Bad Request |
| `401` | Unauthorized |
| `403` | Forbidden |
| `404` | Not Found |
| `413` | Payload Too Large |
| `415` | Unsupported Media Type |
| `429` | Too Many Requests |
| `500` | Internal Server Error |
| `503` | Service Unavailable |

### Error Response Format

```json
{
  "error": {
    "code": "INVALID_AUDIO_FORMAT",
    "message": "The uploaded file format is not supported",
    "details": "Supported formats: wav, mp3, m4a, flac",
    "timestamp": "2023-12-01T10:00:00Z",
    "request_id": "req_123456789"
  }
}
```

### Error Codes

| Code | Description | HTTP Status |
|------|-------------|-------------|
| `INVALID_API_KEY` | Invalid or missing API key | 401 |
| `RATE_LIMIT_EXCEEDED` | Too many requests | 429 |
| `FILE_TOO_LARGE` | File exceeds size limit | 413 |
| `INVALID_AUDIO_FORMAT` | Unsupported audio format | 400 |
| `INVALID_LANGUAGE` | Unsupported language | 400 |
| `INVALID_MODEL` | Unsupported model | 400 |
| `TRANSCRIPTION_NOT_FOUND` | Transcription job not found | 404 |
| `TRANSCRIPTION_FAILED` | Processing failed | 500 |
| `SERVICE_UNAVAILABLE` | Transcription service down | 503 |

## 🚦 Rate Limiting

### Limits

| Tier | Requests per minute | Requests per hour | Concurrent uploads |
|------|-------------------|------------------|-------------------|
| Free | 10 | 100 | 1 |
| Basic | 60 | 1,000 | 3 |
| Pro | 300 | 10,000 | 10 |
| Enterprise | 1,000 | 50,000 | 50 |

### Rate Limit Headers

```http
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 59
X-RateLimit-Reset: 1640995260
X-RateLimit-Tier: basic
```

### Rate Limit Exceeded Response

```json
{
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Rate limit exceeded",
    "details": "You have exceeded your rate limit. Please try again later.",
    "retry_after": 60
  }
}
```

## 🔗 Webhooks/Callbacks

### Webhook Configuration

Configure webhooks to receive notifications when transcription jobs complete:

```http
POST /api/v1/webhooks
```

```json
{
  "url": "https://your-app.com/webhooks/transcription-complete",
  "events": ["transcription.completed", "transcription.failed"],
  "secret": "your-webhook-secret"
}
```

### Webhook Payload

When a transcription completes, a POST request is sent to your webhook URL:

```json
{
  "event": "transcription.completed",
  "timestamp": "2023-12-01T10:05:00Z",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "status": "COMPLETED",
    "transcript_text": "Hello world, this is a test recording.",
    "duration_seconds": 45.67,
    "processing_time_seconds": 23.5,
    "download_url": "/api/v1/transcriptions/550e8400-e29b-41d4-a716-446655440000/download"
  }
}
```

### Webhook Signature Verification

Verify webhook authenticity using HMAC-SHA256:

```python
import hmac
import hashlib

def verify_webhook(payload, signature, secret):
    expected_signature = hmac.new(
        secret.encode(),
        payload.encode(),
        hashlib.sha256
    ).hexdigest()
    
    return hmac.compare_digest(f"sha256={expected_signature}", signature)
```

## 🛠️ SDKs and Integration

### Python SDK

```python
from speechtotext import SpeechToTextClient

# Initialize client
client = SpeechToTextClient(api_key="your-api-key")

# Upload and transcribe (async)
job = client.transcribe_file(
    file_path="audio.wav",
    language="en",
    model="base",
    diarization=True
)

# Wait for completion
result = client.wait_for_completion(job.id, timeout=300)
print(result.transcript_text)

# Download transcript
client.download_transcript(job.id, "transcript.txt", format="txt")
```

### JavaScript SDK

```javascript
const SpeechToTextClient = require('@yourcompany/speechtotext');

const client = new SpeechToTextClient({
  apiKey: 'your-api-key',
  baseURL: 'https://api.yourdomain.com'
});

// Upload and transcribe
const job = await client.transcribeFile({
  file: fileBuffer,
  language: 'en',
  model: 'base',
  sync: false
});

// Poll for completion
const result = await client.waitForCompletion(job.id);
console.log(result.transcript_text);
```

### cURL Examples

#### Sync Transcription
```bash
curl -X POST "https://api.yourdomain.com/api/v1/transcriptions" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -F "file=@short-audio.wav" \
  -F "language=en" \
  -F "sync=true"
```

#### Async Transcription with Polling
```bash
# Upload file
RESPONSE=$(curl -X POST "https://api.yourdomain.com/api/v1/transcriptions" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -F "file=@long-audio.wav" \
  -F "language=en" \
  -F "sync=false")

JOB_ID=$(echo $RESPONSE | jq -r '.id')

# Poll for completion
while true; do
  STATUS=$(curl -s "https://api.yourdomain.com/api/v1/transcriptions/$JOB_ID" \
    -H "Authorization: Bearer YOUR_API_KEY" | jq -r '.status')
  
  if [ "$STATUS" = "COMPLETED" ]; then
    echo "Transcription completed!"
    break
  elif [ "$STATUS" = "FAILED" ]; then
    echo "Transcription failed!"
    break
  fi
  
  echo "Status: $STATUS"
  sleep 5
done

# Download result
curl "https://api.yourdomain.com/api/v1/transcriptions/$JOB_ID/download" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -o transcript.txt
```

## 🔧 Integration Examples

### Web Application Integration

```html
<!DOCTYPE html>
<html>
<head>
    <title>Speech to Text Upload</title>
</head>
<body>
    <input type="file" id="audioFile" accept=".wav,.mp3,.m4a,.flac">
    <button onclick="uploadFile()">Transcribe</button>
    <div id="status"></div>
    <div id="result"></div>

    <script>
    async function uploadFile() {
        const fileInput = document.getElementById('audioFile');
        const file = fileInput.files[0];
        
        const formData = new FormData();
        formData.append('file', file);
        formData.append('language', 'en');
        formData.append('sync', 'false');
        
        try {
            const response = await fetch('/api/v1/transcriptions', {
                method: 'POST',
                headers: {
                    'Authorization': 'Bearer YOUR_API_KEY'
                },
                body: formData
            });
            
            const job = await response.json();
            document.getElementById('status').textContent = 'Processing...';
            
            // Poll for completion
            pollForCompletion(job.id);
        } catch (error) {
            console.error('Error:', error);
        }
    }
    
    async function pollForCompletion(jobId) {
        const response = await fetch(`/api/v1/transcriptions/${jobId}`, {
            headers: {
                'Authorization': 'Bearer YOUR_API_KEY'
            }
        });
        
        const result = await response.json();
        
        if (result.status === 'COMPLETED') {
            document.getElementById('status').textContent = 'Completed!';
            document.getElementById('result').textContent = result.transcript_text;
        } else if (result.status === 'FAILED') {
            document.getElementById('status').textContent = 'Failed: ' + result.error_message;
        } else {
            setTimeout(() => pollForCompletion(jobId), 2000);
        }
    }
    </script>
</body>
</html>
```

### Mobile App Integration (React Native)

```javascript
import React, { useState } from 'react';
import { View, Button, Text } from 'react-native';
import DocumentPicker from 'react-native-document-picker';

const TranscriptionScreen = () => {
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  const pickAndTranscribe = async () => {
    try {
      const file = await DocumentPicker.pickSingle({
        type: [DocumentPicker.types.audio],
      });

      setLoading(true);

      const formData = new FormData();
      formData.append('file', {
        uri: file.uri,
        type: file.type,
        name: file.name,
      });
      formData.append('language', 'en');
      formData.append('sync', 'false');

      const response = await fetch('https://api.yourdomain.com/api/v1/transcriptions', {
        method: 'POST',
        headers: {
          'Authorization': 'Bearer YOUR_API_KEY',
          'Content-Type': 'multipart/form-data',
        },
        body: formData,
      });

      const job = await response.json();
      
      // Poll for completion
      const finalResult = await pollForResult(job.id);
      setResult(finalResult.transcript_text);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const pollForResult = async (jobId) => {
    while (true) {
      const response = await fetch(`https://api.yourdomain.com/api/v1/transcriptions/${jobId}`, {
        headers: {
          'Authorization': 'Bearer YOUR_API_KEY',
        },
      });

      const result = await response.json();

      if (result.status === 'COMPLETED') {
        return result;
      } else if (result.status === 'FAILED') {
        throw new Error(result.error_message);
      }

      await new Promise(resolve => setTimeout(resolve, 2000));
    }
  };

  return (
    <View>
      <Button title="Pick Audio File" onPress={pickAndTranscribe} disabled={loading} />
      {loading && <Text>Processing...</Text>}
      {result && <Text>{result}</Text>}
    </View>
  );
};
```

### Batch Processing Script

```python
import os
import time
import requests
from pathlib import Path

API_KEY = "your-api-key"
BASE_URL = "https://api.yourdomain.com/api/v1"

def transcribe_batch(audio_folder, output_folder):
    """Transcribe all audio files in a folder"""
    
    audio_files = list(Path(audio_folder).glob("*.wav")) + \
                  list(Path(audio_folder).glob("*.mp3")) + \
                  list(Path(audio_folder).glob("*.m4a"))
    
    jobs = []
    
    # Upload all files
    for audio_file in audio_files:
        print(f"Uploading {audio_file.name}...")
        
        with open(audio_file, 'rb') as f:
            files = {'file': f}
            data = {
                'language': 'en',
                'model': 'base',
                'sync': 'false'
            }
            headers = {'Authorization': f'Bearer {API_KEY}'}
            
            response = requests.post(
                f"{BASE_URL}/transcriptions",
                files=files,
                data=data,
                headers=headers
            )
            
            if response.status_code == 202:
                job = response.json()
                jobs.append((job['id'], audio_file.name))
                print(f"Job {job['id']} created for {audio_file.name}")
            else:
                print(f"Error uploading {audio_file.name}: {response.text}")
    
    # Wait for all jobs to complete
    completed_jobs = []
    while jobs:
        for job_id, filename in jobs[:]:
            headers = {'Authorization': f'Bearer {API_KEY}'}
            response = requests.get(f"{BASE_URL}/transcriptions/{job_id}", headers=headers)
            
            if response.status_code == 200:
                result = response.json()
                
                if result['status'] == 'COMPLETED':
                    print(f"Completed: {filename}")
                    
                    # Download transcript
                    download_response = requests.get(
                        f"{BASE_URL}/transcriptions/{job_id}/download",
                        headers=headers
                    )
                    
                    output_file = Path(output_folder) / f"{filename}.txt"
                    with open(output_file, 'w') as f:
                        f.write(download_response.text)
                    
                    jobs.remove((job_id, filename))
                    completed_jobs.append((job_id, filename))
                
                elif result['status'] == 'FAILED':
                    print(f"Failed: {filename} - {result.get('error_message')}")
                    jobs.remove((job_id, filename))
        
        if jobs:
            time.sleep(10)  # Wait before polling again
    
    print(f"Batch processing completed. {len(completed_jobs)} files transcribed.")

if __name__ == "__main__":
    transcribe_batch("./audio_files", "./transcripts")
```

---

This comprehensive API documentation provides all the necessary information for developers to integrate with the Speech to Text service effectively. Regular updates should be made as new features are added or endpoints are modified.

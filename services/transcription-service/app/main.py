"""
FastAPI application for audio transcription using WhisperX.
"""
import os
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from prometheus_client import make_asgi_app

from .worker.transcribe import TranscriptionService
from .models.schemas import TranscriptionRequest, TranscriptionResponse
from .storage.s3_adapter import S3StorageAdapter

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

logger = logging.getLogger(__name__)

# Global transcription service instance
transcription_service = None
s3_storage = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan events."""
    global transcription_service, s3_storage
    
    logger.info("Starting transcription service...")
    
    try:
        # Initialize S3 storage
        s3_storage = S3StorageAdapter()
        
        # Initialize transcription service with WhisperX
        transcription_service = TranscriptionService(s3_storage)
        await transcription_service.initialize()
        
        logger.info("Transcription service initialized successfully")
        
    except Exception as e:
        logger.error(f"Failed to initialize transcription service: {e}")
        raise
    
    yield
    
    logger.info("Shutting down transcription service...")
    if transcription_service:
        await transcription_service.cleanup()

# Create FastAPI app
app = FastAPI(
    title="Speech to Text Transcription Service",
    description="WhisperX-based audio transcription service with alignment and diarization",
    version="1.0.0",
    lifespan=lifespan
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # In production, specify actual origins
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Add Prometheus metrics endpoint
metrics_app = make_asgi_app()
app.mount("/metrics", metrics_app)

@app.get("/")
async def root():
    """Root endpoint."""
    return {
        "message": "Speech to Text Transcription Service", 
        "status": "running",
        "version": "1.0.0",
        "features": ["transcription", "alignment", "diarization"]
    }

@app.get("/health")
async def health_check():
    """Health check endpoint."""
    # Check if transcription service is properly initialized
    if transcription_service and transcription_service.is_ready():
        return {"status": "healthy", "service": "transcription-service", "whisperx": "loaded"}
    else:
        raise HTTPException(status_code=503, detail="Service not ready")

@app.post("/transcribe", response_model=TranscriptionResponse)
async def transcribe_audio(
    request: TranscriptionRequest,
    background_tasks: BackgroundTasks
):
    """
    Process audio transcription with WhisperX.
    Supports both sync and async processing via background tasks.
    """
    logger.info(f"Received transcription request for job: {request.job_id}")
    
    if not transcription_service:
        raise HTTPException(status_code=503, detail="Transcription service not available")
    
    try:
        # For async processing, use background tasks
        if not request.synchronous:
            background_tasks.add_task(
                transcription_service.process_transcription_async, 
                request
            )
            return TranscriptionResponse(
                job_id=request.job_id,
                status="PROCESSING",
                message="Transcription started in background"
            )
        else:
            # Synchronous processing
            result = await transcription_service.process_transcription_sync(request)
            return result
            
    except Exception as e:
        logger.error(f"Error processing transcription request: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/transcribe-upload")
async def transcribe_upload():
    """
    Direct file upload endpoint for transcription.
    Alternative to S3-based processing.
    """
    # This will be implemented if needed for direct uploads
    return {"message": "Direct upload transcription endpoint", "status": "not_implemented"}

@app.get("/models")
async def list_available_models():
    """List available Whisper models."""
    if transcription_service:
        return {"models": transcription_service.get_available_models()}
    else:
        return {"models": ["tiny", "base", "small", "medium", "large"]}

if __name__ == "__main__":
    import uvicorn
    
    port = int(os.getenv("PORT", 8081))
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=port,
        reload=os.getenv("ENVIRONMENT") == "development"
    )

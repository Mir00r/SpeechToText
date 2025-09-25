"""
FastAPI application for audio transcription using WhisperX.
"""
import os
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from prometheus_client import make_asgi_app

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

logger = logging.getLogger(__name__)

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan events."""
    logger.info("Starting transcription service...")
    yield
    logger.info("Shutting down transcription service...")

# Create FastAPI app
app = FastAPI(
    title="Speech to Text Transcription Service",
    description="WhisperX-based audio transcription service",
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
    return {"message": "Speech to Text Transcription Service", "status": "running"}

@app.get("/health")
async def health_check():
    """Health check endpoint."""
    return {"status": "healthy", "service": "transcription-service"}

# Placeholder transcription endpoint for M1
@app.post("/transcribe")
async def transcribe_audio():
    """Transcribe audio endpoint (placeholder for M1)."""
    return {"message": "Transcription endpoint ready", "status": "not_implemented"}

if __name__ == "__main__":
    import uvicorn
    
    port = int(os.getenv("PORT", 8081))
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=port,
        reload=os.getenv("ENVIRONMENT") == "development"
    )

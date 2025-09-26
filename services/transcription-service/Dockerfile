# Multi-stage Dockerfile for transcription service
# Supports both CPU and GPU environments

FROM python:3.11-slim as base

# Install system dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    git \
    ffmpeg \
    sox \
    libsndfile1 \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN useradd --create-home --shell /bin/bash app
USER app
WORKDIR /home/app

# Set Python path
ENV PYTHONPATH=/home/app
ENV PATH="/home/app/.local/bin:$PATH"

# Copy requirements first for better caching
COPY --chown=app:app requirements.txt ./
RUN pip install --user --no-cache-dir -r requirements.txt

# Copy application code
COPY --chown=app:app app/ ./app/
COPY --chown=app:app tests/ ./tests/

# Create directories for models and cache
RUN mkdir -p /home/app/.cache/whisper /home/app/.cache/huggingface

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD python -c "import requests; requests.get('http://localhost:8081/health')"

# Default CMD - can be overridden for GPU
CMD ["python", "-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8081"]

# Optional GPU support instructions (commented out for CPU-only setup)
# For GPU support, use:
# FROM nvidia/cuda:12.1-runtime-ubuntu20.04
# And install CUDA-compatible PyTorch:
# RUN pip install torch==2.1.0+cu121 torchaudio==2.1.0+cu121 --index-url https://download.pytorch.org/whl/cu121

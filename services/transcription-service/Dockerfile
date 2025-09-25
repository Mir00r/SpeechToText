# Use Python 3.11 slim image
FROM python:3.11-slim

# Set working directory
WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    ffmpeg \
    git \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first for better caching
COPY requirements.txt .

# Install Python dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Create non-root user
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# Copy application code
COPY app/ app/

# Change ownership
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/health || exit 1

# Run the application
CMD ["python", "-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8081"]

# Optional GPU support instructions (commented out for CPU-only setup)
# For GPU support, use:
# FROM nvidia/cuda:12.1-runtime-ubuntu20.04
# And install CUDA-compatible PyTorch:
# RUN pip install torch==2.1.0+cu121 torchaudio==2.1.0+cu121 --index-url https://download.pytorch.org/whl/cu121

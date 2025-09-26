"""
WhisperX transcription worker with alignment and diarization support.
"""
import os
import json
import time
import logging
import asyncio
import tempfile
import requests
from typing import Optional, Dict, Any, List
from uuid import UUID
import torch
import whisperx

from ..models.schemas import (
    TranscriptionRequest, 
    TranscriptionResponse, 
    TranscriptionStatus,
    TranscriptionCallback
)
from ..storage.s3_adapter import S3StorageAdapter

logger = logging.getLogger(__name__)

class TranscriptionService:
    """WhisperX-based transcription service with alignment and diarization."""
    
    def __init__(self, s3_adapter: S3StorageAdapter):
        self.s3_adapter = s3_adapter
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        self.models = {}  # Cache for loaded models
        self.alignment_models = {}  # Cache for alignment models
        self.diarization_pipeline = None
        
        # Configuration
        self.hf_token = os.getenv('HUGGINGFACE_TOKEN')
        self.max_retries = int(os.getenv('MAX_RETRIES', '3'))
        self.callback_timeout = int(os.getenv('CALLBACK_TIMEOUT', '30'))
        
        logger.info(f"Transcription service initialized on device: {self.device}")
        if torch.cuda.is_available():
            logger.info(f"GPU available: {torch.cuda.get_device_name()}")
    
    async def initialize(self):
        """Initialize the transcription service and load default models."""
        try:
            logger.info("Loading default WhisperX model...")
            
            # Load default model (base) for faster startup
            await asyncio.get_event_loop().run_in_executor(
                None, 
                self._load_whisper_model, 
                "base"
            )
            
            logger.info("Transcription service initialized successfully")
            
        except Exception as e:
            logger.error(f"Failed to initialize transcription service: {e}")
            raise
    
    def _load_whisper_model(self, model_name: str, compute_type: str = "float16"):
        """Load WhisperX model."""
        cache_key = f"{model_name}_{compute_type}"
        
        if cache_key in self.models:
            return self.models[cache_key]
        
        logger.info(f"Loading Whisper model: {model_name} with compute_type: {compute_type}")
        
        try:
            model = whisperx.load_model(
                model_name,
                device=self.device,
                compute_type=compute_type,
                language=None  # Auto-detect
            )
            
            self.models[cache_key] = model
            logger.info(f"Successfully loaded model: {model_name}")
            return model
            
        except Exception as e:
            logger.error(f"Failed to load model {model_name}: {e}")
            raise
    
    def _load_alignment_model(self, language: str):
        """Load alignment model for specific language."""
        if language in self.alignment_models:
            return self.alignment_models[language]
        
        logger.info(f"Loading alignment model for language: {language}")
        
        try:
            model, metadata = whisperx.load_align_model(
                language_code=language, 
                device=self.device
            )
            
            self.alignment_models[language] = (model, metadata)
            logger.info(f"Successfully loaded alignment model for: {language}")
            return model, metadata
            
        except Exception as e:
            logger.error(f"Failed to load alignment model for {language}: {e}")
            return None, None
    
    def _load_diarization_pipeline(self):
        """Load speaker diarization pipeline."""
        if self.diarization_pipeline:
            return self.diarization_pipeline
        
        if not self.hf_token:
            logger.warning("HuggingFace token not provided, diarization will be unavailable")
            return None
        
        logger.info("Loading diarization pipeline...")
        
        try:
            self.diarization_pipeline = whisperx.DiarizationPipeline(
                use_auth_token=self.hf_token,
                device=self.device
            )
            
            logger.info("Successfully loaded diarization pipeline")
            return self.diarization_pipeline
            
        except Exception as e:
            logger.error(f"Failed to load diarization pipeline: {e}")
            return None
    
    async def process_transcription_async(self, request: TranscriptionRequest):
        """Process transcription asynchronously and send callback."""
        logger.info(f"Starting async transcription for job: {request.job_id}")
        
        try:
            # Process transcription
            result = await self.process_transcription_sync(request)
            
            # Send callback if URL provided
            if request.callback_url:
                await self._send_callback(request.callback_url, request.job_id, result)
                
        except Exception as e:
            logger.error(f"Async transcription failed for job {request.job_id}: {e}")
            
            # Send failure callback
            if request.callback_url:
                error_result = TranscriptionResponse(
                    job_id=request.job_id,
                    status=TranscriptionStatus.FAILED,
                    message="Transcription failed",
                    error_message=str(e)
                )
                await self._send_callback(request.callback_url, request.job_id, error_result)
    
    async def process_transcription_sync(self, request: TranscriptionRequest) -> TranscriptionResponse:
        """Process transcription synchronously and return result."""
        start_time = time.time()
        logger.info(f"Processing transcription for job: {request.job_id}")
        
        local_audio_path = None
        
        try:
            # Download audio file from S3
            local_audio_path = self.s3_adapter.download_file(request.s3_url)
            
            # Load Whisper model
            model = await asyncio.get_event_loop().run_in_executor(
                None,
                self._load_whisper_model,
                request.model.value,
                request.compute_type.value
            )
            
            # Transcribe audio
            logger.info(f"Transcribing audio with model: {request.model.value}")
            result = await asyncio.get_event_loop().run_in_executor(
                None,
                model.transcribe,
                local_audio_path,
                {"batch_size": request.batch_size}
            )
            
            language = result.get("language", request.language)
            logger.info(f"Detected/used language: {language}")
            
            # Perform alignment if language detected
            aligned_result = result
            if language and language != "auto":
                try:
                    align_model, metadata = await asyncio.get_event_loop().run_in_executor(
                        None,
                        self._load_alignment_model,
                        language
                    )
                    
                    if align_model:
                        logger.info("Performing word-level alignment")
                        aligned_result = await asyncio.get_event_loop().run_in_executor(
                            None,
                            whisperx.align,
                            result["segments"],
                            align_model,
                            metadata,
                            local_audio_path,
                            self.device,
                            return_char_alignments=False
                        )
                except Exception as e:
                    logger.warning(f"Alignment failed: {e}, continuing without alignment")
            
            # Perform diarization if requested
            diarized_result = aligned_result
            speakers = None
            if request.diarize:
                try:
                    diarization_pipeline = await asyncio.get_event_loop().run_in_executor(
                        None,
                        self._load_diarization_pipeline
                    )
                    
                    if diarization_pipeline:
                        logger.info("Performing speaker diarization")
                        diarization_result = await asyncio.get_event_loop().run_in_executor(
                            None,
                            diarization_pipeline,
                            local_audio_path
                        )
                        
                        diarized_result = await asyncio.get_event_loop().run_in_executor(
                            None,
                            whisperx.assign_word_speakers,
                            diarization_result,
                            aligned_result
                        )
                        
                        # Extract speaker information
                        speakers = self._extract_speaker_info(diarized_result.get("segments", []))
                        
                except Exception as e:
                    logger.warning(f"Diarization failed: {e}, continuing without diarization")
            
            # Extract transcript and segments
            transcript_text = " ".join([seg.get("text", "").strip() for seg in diarized_result.get("segments", [])])
            segments = diarized_result.get("segments", [])
            word_segments = diarized_result.get("word_segments", [])
            
            # Calculate average confidence
            confidences = [seg.get("confidence", 0.0) for seg in segments if "confidence" in seg]
            avg_confidence = sum(confidences) / len(confidences) if confidences else None
            
            # Upload results to S3
            transcript_url = None
            timestamps_url = None
            
            try:
                job_id_str = str(request.job_id)
                
                # Upload transcript
                transcript_key = f"transcripts/{job_id_str}.txt"
                transcript_url = self.s3_adapter.upload_text_file(transcript_text, transcript_key)
                
                # Upload timestamps/segments
                timestamps_data = {
                    "segments": segments,
                    "word_segments": word_segments,
                    "speakers": speakers,
                    "language": language,
                    "model": request.model.value,
                    "confidence": avg_confidence
                }
                
                timestamps_key = f"timestamps/{job_id_str}.json"
                timestamps_url = self.s3_adapter.upload_json_file(timestamps_data, timestamps_key)
                
            except Exception as e:
                logger.error(f"Failed to upload results to S3: {e}")
            
            processing_time = time.time() - start_time
            
            # Create success response
            response = TranscriptionResponse(
                job_id=request.job_id,
                status=TranscriptionStatus.COMPLETED,
                message="Transcription completed successfully",
                transcript_text=transcript_text,
                confidence=avg_confidence,
                language_detected=language,
                segments=segments,
                word_segments=word_segments,
                speakers=speakers,
                processing_time=processing_time,
                model_used=request.model.value,
                compute_type_used=request.compute_type.value,
                transcript_url=transcript_url,
                timestamps_url=timestamps_url
            )
            
            logger.info(f"Transcription completed for job {request.job_id} in {processing_time:.2f}s")
            return response
            
        except Exception as e:
            processing_time = time.time() - start_time
            logger.error(f"Transcription failed for job {request.job_id}: {e}")
            
            return TranscriptionResponse(
                job_id=request.job_id,
                status=TranscriptionStatus.FAILED,
                message="Transcription failed",
                error_message=str(e),
                error_code="TRANSCRIPTION_ERROR",
                processing_time=processing_time
            )
            
        finally:
            # Clean up temporary file
            if local_audio_path:
                self.s3_adapter.cleanup_temp_file(local_audio_path)
    
    def _extract_speaker_info(self, segments: List[Dict]) -> List[Dict]:
        """Extract speaker information from diarized segments."""
        speakers = {}
        
        for segment in segments:
            if "speaker" in segment:
                speaker_id = segment["speaker"]
                if speaker_id not in speakers:
                    speakers[speaker_id] = {
                        "id": speaker_id,
                        "total_speech_time": 0.0,
                        "segments_count": 0
                    }
                
                speakers[speaker_id]["total_speech_time"] += segment.get("end", 0) - segment.get("start", 0)
                speakers[speaker_id]["segments_count"] += 1
        
        return list(speakers.values())
    
    async def _send_callback(self, callback_url: str, job_id: UUID, result: TranscriptionResponse):
        """Send callback to API service."""
        logger.info(f"Sending callback for job {job_id} to {callback_url}")
        
        try:
            # Create callback payload
            callback_data = TranscriptionCallback(
                job_id=job_id,
                status=result.status,
                transcript_text=result.transcript_text,
                timestamps_json=json.dumps({
                    "segments": result.segments,
                    "word_segments": result.word_segments,
                    "speakers": result.speakers
                }) if result.segments else None,
                error_message=result.error_message,
                processing_time=result.processing_time,
                language_detected=result.language_detected,
                confidence=result.confidence
            )
            
            # Send callback with retries
            for attempt in range(self.max_retries):
                try:
                    response = requests.post(
                        f"{callback_url}/{job_id}/callback",
                        json=callback_data.dict(),
                        timeout=self.callback_timeout
                    )
                    response.raise_for_status()
                    
                    logger.info(f"Callback sent successfully for job {job_id}")
                    return
                    
                except requests.RequestException as e:
                    logger.warning(f"Callback attempt {attempt + 1} failed: {e}")
                    if attempt < self.max_retries - 1:
                        await asyncio.sleep(2 ** attempt)  # Exponential backoff
            
            logger.error(f"Failed to send callback for job {job_id} after {self.max_retries} attempts")
            
        except Exception as e:
            logger.error(f"Error sending callback for job {job_id}: {e}")
    
    def is_ready(self) -> bool:
        """Check if service is ready to process requests."""
        return len(self.models) > 0
    
    def get_available_models(self) -> List[str]:
        """Get list of available Whisper models."""
        return ["tiny", "base", "small", "medium", "large"]
    
    async def cleanup(self):
        """Clean up resources."""
        logger.info("Cleaning up transcription service...")
        
        # Clear model cache
        self.models.clear()
        self.alignment_models.clear()
        self.diarization_pipeline = None
        
        # Clear CUDA cache if available
        if torch.cuda.is_available():
            torch.cuda.empty_cache()
        
        logger.info("Transcription service cleanup completed")
import os
import logging
from typing import Optional, Dict, Any
from pathlib import Path

logger = logging.getLogger(__name__)

class TranscriptionWorker:
    """WhisperX transcription worker."""
    
    def __init__(self):
        """Initialize transcription worker."""
        self.device = "cuda" if self._is_cuda_available() else "cpu"
        self.compute_type = os.getenv("WHISPER_COMPUTE_TYPE", "float16")
        logger.info(f"TranscriptionWorker initialized with device: {self.device}")
    
    def _is_cuda_available(self) -> bool:
        """Check if CUDA is available."""
        try:
            # Placeholder for M1 - will implement in M3
            return False
        except ImportError:
            return False
    
    async def transcribe(
        self, 
        audio_path: str, 
        language: Optional[str] = None,
        model: str = "base",
        diarize: bool = False
    ) -> Dict[str, Any]:
        """Transcribe audio file using WhisperX."""
        logger.info(f"Transcribing {audio_path} with model {model}")
        
        # Placeholder implementation for M1
        result = {
            "text": "Sample transcription text for testing",
            "segments": [
                {
                    "start": 0.0,
                    "end": 3.0,
                    "text": "Sample transcription text for testing",
                    "speaker": "SPEAKER_00" if diarize else None
                }
            ],
            "language": language or "en"
        }
        
        logger.info("Transcription completed")
        return result
    
    def get_supported_models(self) -> list:
        """Get list of supported WhisperX models."""
        return ["tiny", "base", "small", "medium", "large-v2", "large-v3"]

import pytest
import asyncio
from fastapi.testclient import TestClient
from unittest.mock import Mock, patch, AsyncMock
from app.main import app

client = TestClient(app)


class TestTranscriptionServicePerformance:
    """Performance tests for the transcription service."""

    @pytest.mark.asyncio
    async def test_concurrent_transcription_requests(self):
        """Test handling multiple concurrent transcription requests."""
        
        @patch('app.main.transcription_service.transcribe_audio')
        async def run_test(mock_transcribe):
            # Mock the transcription service
            mock_transcribe.return_value = {
                "status": "completed",
                "transcript_text": "Test transcript",
                "segments": [],
                "metadata": {}
            }
            
            # Create multiple concurrent requests
            async def make_request():
                response = client.post(
                    "/transcribe",
                    json={
                        "job_id": "test-job-123",
                        "s3_url": "s3://bucket/test.wav",
                        "callback_url": "http://localhost:8080/callback",
                        "language": "en",
                        "model": "base"
                    }
                )
                return response.status_code
                
            # Run 10 concurrent requests
            tasks = [make_request() for _ in range(10)]
            results = await asyncio.gather(*tasks)
            
            # All requests should succeed
            assert all(status == 200 for status in results)
            
        await run_test()

    def test_large_payload_handling(self):
        """Test handling of large transcription payloads."""
        
        # Create a large segments list to simulate big transcription results
        large_segments = []
        for i in range(1000):
            large_segments.append({
                "start": i * 0.1,
                "end": (i + 1) * 0.1,
                "text": f"Word number {i}",
                "speaker": f"SPEAKER_{i % 3}"
            })
        
        with patch('app.main.transcription_service.transcribe_audio') as mock_transcribe:
            mock_transcribe.return_value = {
                "status": "completed", 
                "transcript_text": "Large transcript with many segments",
                "segments": large_segments,
                "metadata": {"duration": 100.0}
            }
            
            response = client.post(
                "/transcribe",
                json={
                    "job_id": "large-job-123",
                    "s3_url": "s3://bucket/large.wav", 
                    "callback_url": "http://localhost:8080/callback",
                    "language": "en",
                    "model": "base"
                }
            )
            
            assert response.status_code == 200
            result = response.json()
            assert len(result["segments"]) == 1000

    def test_memory_cleanup_after_processing(self):
        """Test that memory is properly cleaned up after transcription."""
        import gc
        import sys
        
        initial_objects = len(gc.get_objects())
        
        with patch('app.main.transcription_service.transcribe_audio') as mock_transcribe:
            mock_transcribe.return_value = {
                "status": "completed",
                "transcript_text": "Test transcript",
                "segments": [],
                "metadata": {}
            }
            
            # Process multiple requests
            for i in range(50):
                response = client.post(
                    "/transcribe",
                    json={
                        "job_id": f"memory-test-{i}",
                        "s3_url": "s3://bucket/test.wav",
                        "callback_url": "http://localhost:8080/callback", 
                        "language": "en",
                        "model": "base"
                    }
                )
                assert response.status_code == 200
        
        # Force garbage collection
        gc.collect()
        
        final_objects = len(gc.get_objects())
        
        # Memory should not have grown significantly
        object_growth = final_objects - initial_objects
        assert object_growth < 1000, f"Too many objects created: {object_growth}"

    def test_error_handling_performance(self):
        """Test that error handling doesn't cause performance issues."""
        import time
        
        with patch('app.main.transcription_service.transcribe_audio') as mock_transcribe:
            mock_transcribe.side_effect = Exception("Transcription failed")
            
            start_time = time.time()
            
            # Make multiple requests that will fail
            for i in range(10):
                response = client.post(
                    "/transcribe",
                    json={
                        "job_id": f"error-test-{i}",
                        "s3_url": "s3://bucket/test.wav",
                        "callback_url": "http://localhost:8080/callback",
                        "language": "en", 
                        "model": "base"
                    }
                )
                # Should handle errors gracefully
                assert response.status_code == 500
            
            end_time = time.time()
            duration = end_time - start_time
            
            # Error handling should be fast
            assert duration < 5.0, f"Error handling took too long: {duration}s"


class TestTranscriptionServiceLoad:
    """Load testing for the transcription service."""
    
    def test_health_endpoint_under_load(self):
        """Test health endpoint can handle high load."""
        import time
        import threading
        
        results = []
        
        def make_health_request():
            response = client.get("/health")
            results.append(response.status_code)
        
        # Create 100 threads hitting health endpoint
        threads = []
        start_time = time.time()
        
        for _ in range(100):
            thread = threading.Thread(target=make_health_request)
            threads.append(thread)
            thread.start()
        
        # Wait for all threads to complete
        for thread in threads:
            thread.join()
        
        end_time = time.time()
        duration = end_time - start_time
        
        # All requests should succeed
        assert all(status == 200 for status in results)
        assert len(results) == 100
        
        # Should complete within reasonable time
        assert duration < 10.0, f"Health endpoint load test took too long: {duration}s"
        
        print(f"Health endpoint load test: {len(results)} requests in {duration:.2f}s")
        print(f"Average response time: {(duration / len(results)) * 1000:.2f}ms")

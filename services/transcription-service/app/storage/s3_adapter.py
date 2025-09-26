"""
S3/MinIO storage adapter for audio files and transcripts.
"""
import os
import logging
import tempfile
import json
from typing import Optional, BinaryIO
from urllib.parse import urlparse
import boto3
from botocore.exceptions import ClientError, NoCredentialsError

logger = logging.getLogger(__name__)

class S3StorageAdapter:
    """Adapter for S3/MinIO storage operations."""
    
    def __init__(self):
        """Initialize S3 client with configuration from environment."""
        self.endpoint_url = os.getenv('S3_ENDPOINT', 'http://localhost:9000')
        self.access_key = os.getenv('S3_ACCESS_KEY', 'minioadmin')
        self.secret_key = os.getenv('S3_SECRET_KEY', 'minioadmin')
        self.bucket_name = os.getenv('S3_BUCKET_NAME', 'speechtotext-bucket')
        self.region = os.getenv('S3_REGION', 'us-east-1')
        
        try:
            self.s3_client = boto3.client(
                's3',
                endpoint_url=self.endpoint_url,
                aws_access_key_id=self.access_key,
                aws_secret_access_key=self.secret_key,
                region_name=self.region,
                use_ssl=self.endpoint_url.startswith('https')
            )
            
            # Verify connection and create bucket if needed
            self._ensure_bucket_exists()
            logger.info(f"S3 adapter initialized successfully with endpoint: {self.endpoint_url}")
            
        except Exception as e:
            logger.error(f"Failed to initialize S3 client: {e}")
            raise
    
    def _ensure_bucket_exists(self):
        """Ensure the bucket exists, create if it doesn't."""
        try:
            self.s3_client.head_bucket(Bucket=self.bucket_name)
            logger.debug(f"Bucket {self.bucket_name} exists")
        except ClientError as e:
            error_code = e.response['Error']['Code']
            if error_code == '404':
                logger.info(f"Creating bucket: {self.bucket_name}")
                self.s3_client.create_bucket(Bucket=self.bucket_name)
            else:
                raise
    
    def download_file(self, s3_url: str, local_path: Optional[str] = None) -> str:
        """
        Download a file from S3 to local storage.
        
        Args:
            s3_url: S3 URL in format s3://bucket/key
            local_path: Optional local path, if None creates temporary file
            
        Returns:
            Path to downloaded local file
        """
        try:
            # Parse S3 URL
            parsed = urlparse(s3_url)
            if parsed.scheme != 's3':
                raise ValueError(f"Invalid S3 URL format: {s3_url}")
            
            bucket = parsed.netloc
            key = parsed.path.lstrip('/')
            
            if not local_path:
                # Create temporary file with appropriate extension
                suffix = os.path.splitext(key)[1] or '.tmp'
                temp_fd, local_path = tempfile.mkstemp(suffix=suffix, prefix='audio_')
                os.close(temp_fd)
            
            logger.info(f"Downloading {s3_url} to {local_path}")
            self.s3_client.download_file(bucket, key, local_path)
            
            return local_path
            
        except Exception as e:
            logger.error(f"Failed to download file from {s3_url}: {e}")
            raise
    
    def upload_text_file(self, content: str, key: str) -> str:
        """
        Upload text content to S3.
        
        Args:
            content: Text content to upload
            key: S3 key (path) for the file
            
        Returns:
            S3 URL of uploaded file
        """
        try:
            logger.info(f"Uploading text file to s3://{self.bucket_name}/{key}")
            
            self.s3_client.put_object(
                Bucket=self.bucket_name,
                Key=key,
                Body=content.encode('utf-8'),
                ContentType='text/plain; charset=utf-8'
            )
            
            s3_url = f"s3://{self.bucket_name}/{key}"
            logger.info(f"Successfully uploaded text file to {s3_url}")
            
            return s3_url
            
        except Exception as e:
            logger.error(f"Failed to upload text file to {key}: {e}")
            raise
    
    def upload_json_file(self, data: dict, key: str) -> str:
        """
        Upload JSON data to S3.
        
        Args:
            data: Dictionary to upload as JSON
            key: S3 key (path) for the file
            
        Returns:
            S3 URL of uploaded file
        """
        try:
            logger.info(f"Uploading JSON file to s3://{self.bucket_name}/{key}")
            
            json_content = json.dumps(data, indent=2, ensure_ascii=False)
            
            self.s3_client.put_object(
                Bucket=self.bucket_name,
                Key=key,
                Body=json_content.encode('utf-8'),
                ContentType='application/json; charset=utf-8'
            )
            
            s3_url = f"s3://{self.bucket_name}/{key}"
            logger.info(f"Successfully uploaded JSON file to {s3_url}")
            
            return s3_url
            
        except Exception as e:
            logger.error(f"Failed to upload JSON file to {key}: {e}")
            raise
    
    def delete_file(self, s3_url: str) -> bool:
        """
        Delete a file from S3.
        
        Args:
            s3_url: S3 URL in format s3://bucket/key
            
        Returns:
            True if successful, False otherwise
        """
        try:
            parsed = urlparse(s3_url)
            bucket = parsed.netloc
            key = parsed.path.lstrip('/')
            
            logger.info(f"Deleting file: {s3_url}")
            self.s3_client.delete_object(Bucket=bucket, Key=key)
            
            return True
            
        except Exception as e:
            logger.error(f"Failed to delete file {s3_url}: {e}")
            return False
    
    def file_exists(self, s3_url: str) -> bool:
        """
        Check if a file exists in S3.
        
        Args:
            s3_url: S3 URL in format s3://bucket/key
            
        Returns:
            True if file exists, False otherwise
        """
        try:
            parsed = urlparse(s3_url)
            bucket = parsed.netloc
            key = parsed.path.lstrip('/')
            
            self.s3_client.head_object(Bucket=bucket, Key=key)
            return True
            
        except ClientError as e:
            if e.response['Error']['Code'] == '404':
                return False
            else:
                logger.error(f"Error checking file existence {s3_url}: {e}")
                return False
    
    def get_file_size(self, s3_url: str) -> Optional[int]:
        """
        Get file size in bytes.
        
        Args:
            s3_url: S3 URL in format s3://bucket/key
            
        Returns:
            File size in bytes, or None if error
        """
        try:
            parsed = urlparse(s3_url)
            bucket = parsed.netloc
            key = parsed.path.lstrip('/')
            
            response = self.s3_client.head_object(Bucket=bucket, Key=key)
            return response['ContentLength']
            
        except Exception as e:
            logger.error(f"Failed to get file size for {s3_url}: {e}")
            return None
    
    def cleanup_temp_file(self, local_path: str):
        """Clean up temporary local file."""
        try:
            if os.path.exists(local_path):
                os.remove(local_path)
                logger.debug(f"Cleaned up temporary file: {local_path}")
        except Exception as e:
            logger.warning(f"Failed to cleanup temp file {local_path}: {e}")

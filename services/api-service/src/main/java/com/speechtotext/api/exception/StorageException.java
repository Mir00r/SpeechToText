package com.speechtotext.api.exception;

/**
 * Exception for storage-related operations (S3/MinIO).
 */
public class StorageException extends TranscriptionException {
    
    public StorageException(String message) {
        super("STORAGE_ERROR", message, "Storage operation failed. Please try again later.");
    }
    
    public StorageException(String message, Throwable cause) {
        super("STORAGE_ERROR", message, "Storage operation failed. Please try again later.", cause);
    }
    
    public StorageException(String message, String userMessage) {
        super("STORAGE_ERROR", message, userMessage);
    }
    
    public StorageException(String message, String userMessage, Throwable cause) {
        super("STORAGE_ERROR", message, userMessage, cause);
    }
    
    // Specific storage error types
    public static class FileUploadException extends StorageException {
        public FileUploadException(String filename, Throwable cause) {
            super(
                String.format("Failed to upload file: %s", filename),
                "File upload failed. Please check your internet connection and try again.",
                cause
            );
        }
    }
    
    public static class FileDownloadException extends StorageException {
        public FileDownloadException(String filename, Throwable cause) {
            super(
                String.format("Failed to download file: %s", filename),
                "File download failed. The file may have been moved or deleted.",
                cause
            );
        }
    }
    
    public static class FileNotFoundException extends StorageException {
        public FileNotFoundException(String filename) {
            super(
                String.format("File not found in storage: %s", filename),
                "The requested file could not be found. It may have been deleted or moved."
            );
        }
    }
    
    public static class StorageConnectionException extends StorageException {
        public StorageConnectionException(Throwable cause) {
            super(
                "Failed to connect to storage service",
                "Unable to connect to storage service. Please try again later.",
                cause
            );
        }
    }
    
    public static class InsufficientStorageException extends StorageException {
        public InsufficientStorageException() {
            super(
                "Insufficient storage space available",
                "Storage space is temporarily unavailable. Please try again later or contact support."
            );
        }
    }
    
    public static class PresignedUrlException extends StorageException {
        public PresignedUrlException(String operation, Throwable cause) {
            super(
                String.format("Failed to generate presigned URL for %s", operation),
                "Failed to generate secure file access URL. Please try again.",
                cause
            );
        }
    }
}

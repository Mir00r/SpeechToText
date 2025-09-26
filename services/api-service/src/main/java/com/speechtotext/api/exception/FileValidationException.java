package com.speechtotext.api.exception;

/**
 * Exception for file validation errors during upload and processing.
 */
public class FileValidationException extends TranscriptionException {
    
    public FileValidationException(String message) {
        super("FILE_VALIDATION_ERROR", message);
    }
    
    public FileValidationException(String message, String userMessage) {
        super("FILE_VALIDATION_ERROR", message, userMessage);
    }
    
    public FileValidationException(String message, Throwable cause) {
        super("FILE_VALIDATION_ERROR", message, cause);
    }
    
    // Specific validation error types
    public static class FileTooLargeException extends FileValidationException {
        public FileTooLargeException(long fileSize, long maxSize) {
            super(
                String.format("File size %d bytes exceeds maximum allowed %d bytes", fileSize, maxSize),
                String.format("File size %.1f MB exceeds the maximum limit of %.1f MB. Please use a smaller file or compress the audio.", 
                    fileSize / 1024.0 / 1024.0, maxSize / 1024.0 / 1024.0)
            );
        }
    }
    
    public static class UnsupportedFormatException extends FileValidationException {
        public UnsupportedFormatException(String detectedType) {
            super(
                String.format("Unsupported file format detected: %s", detectedType),
                "Supported audio formats: WAV, MP3, M4A, FLAC, OGG. Please convert your file to a supported format."
            );
        }
    }
    
    public static class EmptyFileException extends FileValidationException {
        public EmptyFileException() {
            super(
                "File is empty or contains no data",
                "The uploaded file appears to be empty. Please select a valid audio file."
            );
        }
    }
    
    public static class CorruptedFileException extends FileValidationException {
        public CorruptedFileException(String reason) {
            super(
                String.format("File appears to be corrupted: %s", reason),
                "The uploaded file appears to be corrupted or unreadable. Please try uploading the file again."
            );
        }
    }
    
    public static class InvalidAudioFormatException extends FileValidationException {
        public InvalidAudioFormatException(String details) {
            super(
                String.format("Invalid audio format: %s", details),
                "The file does not contain valid audio data. Please ensure the file is a proper audio recording."
            );
        }
    }
}

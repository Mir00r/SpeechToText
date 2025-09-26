package com.speechtotext.api.exception;

import java.util.UUID;

/**
 * Exception for business logic violations and processing errors.
 */
public class BusinessLogicException extends TranscriptionException {
    
    public BusinessLogicException(String message) {
        super("BUSINESS_LOGIC_ERROR", message);
    }
    
    public BusinessLogicException(String message, String userMessage) {
        super("BUSINESS_LOGIC_ERROR", message, userMessage);
    }
    
    public BusinessLogicException(String message, Throwable cause) {
        super("BUSINESS_LOGIC_ERROR", message, cause);
    }
    
    // Specific business logic error types
    public static class JobNotFoundException extends BusinessLogicException {
        public JobNotFoundException(UUID jobId) {
            super(
                String.format("Transcription job not found: %s", jobId),
                "The requested transcription job was not found. Please check the job ID."
            );
        }
    }
    
    public static class JobAlreadyProcessedException extends BusinessLogicException {
        public JobAlreadyProcessedException(UUID jobId) {
            super(
                String.format("Job %s has already been processed", jobId),
                "This transcription job has already been completed and cannot be reprocessed."
            );
        }
    }
    
    public static class InvalidJobStateException extends BusinessLogicException {
        public InvalidJobStateException(UUID jobId, String currentState, String requiredState) {
            super(
                String.format("Job %s is in state '%s' but required state is '%s'", jobId, currentState, requiredState),
                String.format("This operation cannot be performed. Job is currently %s.", currentState.toLowerCase())
            );
        }
    }
    
    public static class TranscriptionFailedException extends BusinessLogicException {
        public TranscriptionFailedException(UUID jobId, String reason) {
            super(
                String.format("Transcription failed for job %s: %s", jobId, reason),
                "The transcription process failed. Please try uploading the file again or contact support."
            );
        }
        
        public TranscriptionFailedException(UUID jobId, String reason, Throwable cause) {
            super(
                String.format("Transcription failed for job %s: %s", jobId, reason),
                "The transcription process failed. Please try uploading the file again or contact support."
            );
        }
    }
    
    public static class UnsupportedLanguageException extends BusinessLogicException {
        public UnsupportedLanguageException(String language) {
            super(
                String.format("Unsupported language: %s", language),
                String.format("Language '%s' is not currently supported. Please use a supported language code.", language)
            );
        }
    }
    
    public static class UnsupportedModelException extends BusinessLogicException {
        public UnsupportedModelException(String model) {
            super(
                String.format("Unsupported transcription model: %s", model),
                String.format("Model '%s' is not available. Please use a supported model.", model)
            );
        }
    }
    
    public static class SyncTimeoutException extends BusinessLogicException {
        public SyncTimeoutException(int timeoutSeconds) {
            super(
                String.format("Synchronous transcription timed out after %d seconds", timeoutSeconds),
                String.format("The transcription is taking longer than expected (%d seconds). The job will continue processing asynchronously.", timeoutSeconds)
            );
        }
    }
    
    public static class ConcurrencyLimitException extends BusinessLogicException {
        public ConcurrencyLimitException(int currentJobs, int maxJobs) {
            super(
                String.format("Concurrency limit reached: %d active jobs (max: %d)", currentJobs, maxJobs),
                "Too many transcription jobs are currently being processed. Please try again in a few minutes."
            );
        }
    }
    
    public static class InvalidCallbackException extends BusinessLogicException {
        public InvalidCallbackException(UUID jobId, String reason) {
            super(
                String.format("Invalid callback for job %s: %s", jobId, reason),
                "Invalid transcription callback received."
            );
        }
    }
}

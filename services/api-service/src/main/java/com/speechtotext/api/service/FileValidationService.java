package com.speechtotext.api.service;

import com.speechtotext.api.exception.BusinessLogicException;
import com.speechtotext.api.exception.FileValidationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service for validating uploaded files using domain-specific exceptions.
 * This demonstrates how to use the new exception hierarchy in business logic.
 */
@Service
public class FileValidationService {
    
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList(
        "audio/wav", "audio/mpeg", "audio/mp4", "audio/flac", "audio/ogg"
    );
    
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB
    private static final long MIN_FILE_SIZE = 1024; // 1KB
    
    /**
     * Validate uploaded file for transcription processing.
     * 
     * @param file the uploaded file
     * @throws FileValidationException if file validation fails
     */
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException.EmptyFileException();
        }
        
        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileValidationException.FileTooLargeException(file.getSize(), MAX_FILE_SIZE);
        }
        
        if (file.getSize() < MIN_FILE_SIZE) {
            throw new FileValidationException("File is too small: " + file.getSize() + " bytes", 
                "File appears to be too small to contain audio data. Please ensure the file is a valid audio recording.");
        }
        
        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !SUPPORTED_FORMATS.contains(contentType)) {
            throw new FileValidationException.UnsupportedFormatException(contentType);
        }
        
        // Check filename extension as additional validation
        String filename = file.getOriginalFilename();
        if (filename != null && !hasValidExtension(filename)) {
            throw new FileValidationException.InvalidAudioFormatException(
                "File extension does not match content type: " + filename
            );
        }
    }
    
    /**
     * Validate language parameter.
     * 
     * @param language the language code
     * @throws BusinessLogicException if language is not supported
     */
    public void validateLanguage(String language) {
        if (language == null || language.trim().isEmpty()) {
            throw new BusinessLogicException.UnsupportedLanguageException("null");
        }
        
        // Example supported languages - in real implementation, this would be more comprehensive
        List<String> supportedLanguages = Arrays.asList("en", "es", "fr", "de", "it", "pt", "ja", "ko", "zh");
        if (!supportedLanguages.contains(language.toLowerCase())) {
            throw new BusinessLogicException.UnsupportedLanguageException(language);
        }
    }
    
    /**
     * Validate model parameter.
     * 
     * @param model the transcription model
     * @throws BusinessLogicException if model is not supported
     */
    public void validateModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            return; // Use default model
        }
        
        // Example supported models
        List<String> supportedModels = Arrays.asList("base", "small", "medium", "large", "large-v2", "large-v3");
        if (!supportedModels.contains(model.toLowerCase())) {
            throw new BusinessLogicException.UnsupportedModelException(model);
        }
    }
    
    /**
     * Validate job state for operations.
     * 
     * @param jobId the job ID
     * @param currentState the current job state
     * @param requiredState the required state for the operation
     * @throws BusinessLogicException if job is not in required state
     */
    public void validateJobState(UUID jobId, String currentState, String requiredState) {
        if (!requiredState.equals(currentState)) {
            throw new BusinessLogicException.InvalidJobStateException(jobId, currentState, requiredState);
        }
    }
    
    private boolean hasValidExtension(String filename) {
        String lowerFilename = filename.toLowerCase();
        return lowerFilename.endsWith(".wav") || 
               lowerFilename.endsWith(".mp3") || 
               lowerFilename.endsWith(".m4a") || 
               lowerFilename.endsWith(".flac") || 
               lowerFilename.endsWith(".ogg");
    }
}

package com.speechtotext.api.controller;

import com.speechtotext.api.service.ModelSelectionService;
import com.speechtotext.api.strategy.QualityPreference;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for model selection information and utilities.
 */
@RestController
@RequestMapping("/api/v1/models")
@Tag(name = "Model Selection", description = "Model selection strategy information and utilities")
public class ModelSelectionController {
    
    private final ModelSelectionService modelSelectionService;
    
    public ModelSelectionController(ModelSelectionService modelSelectionService) {
        this.modelSelectionService = modelSelectionService;
    }
    
    @Operation(
        summary = "Get available Whisper models",
        description = "Retrieve list of supported Whisper models with their characteristics"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Models retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAvailableModels() {
        Map<String, Object> response = new HashMap<>();
        
        // Available models with characteristics
        Map<String, Map<String, Object>> models = new HashMap<>();
        
        models.put("tiny", Map.of(
            "name", "tiny",
            "size", "~39 MB",
            "speed", "~32x realtime",
            "accuracy", "Lower",
            "use_case", "Real-time applications, quick previews"
        ));
        
        models.put("base", Map.of(
            "name", "base", 
            "size", "~74 MB",
            "speed", "~16x realtime", 
            "accuracy", "Good",
            "use_case", "General purpose, balanced performance"
        ));
        
        models.put("small", Map.of(
            "name", "small",
            "size", "~244 MB", 
            "speed", "~6x realtime",
            "accuracy", "Better", 
            "use_case", "Higher accuracy needs, production use"
        ));
        
        models.put("medium", Map.of(
            "name", "medium",
            "size", "~769 MB",
            "speed", "~2x realtime",
            "accuracy", "High",
            "use_case", "Professional transcription, important content"
        ));
        
        models.put("large", Map.of(
            "name", "large",
            "size", "~1550 MB", 
            "speed", "~1x realtime",
            "accuracy", "Highest",
            "use_case", "Critical accuracy requirements, offline processing"
        ));
        
        response.put("models", models);
        response.put("default_model", "base");
        response.put("auto_selection_available", true);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Get available model selection strategies",
        description = "Retrieve list of available model selection strategies"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Strategies retrieved successfully")
    })
    @GetMapping("/strategies")
    public ResponseEntity<Map<String, Object>> getAvailableStrategies() {
        Map<String, Object> response = new HashMap<>();
        
        List<String> strategies = modelSelectionService.getAvailableStrategies();
        response.put("strategies", strategies);
        
        // Strategy descriptions
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("DefaultModelSelection", "Balanced selection considering file size, duration, and quality preference");
        descriptions.put("PerformanceOptimized", "Prioritizes processing speed over accuracy");
        descriptions.put("AccuracyFocused", "Prioritizes transcription accuracy over processing speed");
        
        response.put("descriptions", descriptions);
        response.put("default_strategy", "DefaultModelSelection");
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Get quality preferences",
        description = "Retrieve available quality preferences for transcription"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Quality preferences retrieved successfully")
    })
    @GetMapping("/quality-preferences")
    public ResponseEntity<Map<String, Object>> getQualityPreferences() {
        Map<String, Object> response = new HashMap<>();
        
        List<Map<String, Object>> preferences = Arrays.stream(QualityPreference.values())
                .map(pref -> {
                    Map<String, Object> prefMap = new HashMap<>();
                    prefMap.put("key", pref.getKey());
                    prefMap.put("name", pref.name());
                    prefMap.put("description", pref.getDescription());
                    prefMap.put("priority", pref.getPriority());
                    prefMap.put("processing_time_multiplier", pref.getProcessingTimeMultiplier());
                    return prefMap;
                })
                .toList();
        
        response.put("preferences", preferences);
        response.put("default_preference", "balanced");
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Preview model selection",
        description = "Preview which model would be selected for given parameters without creating a transcription job"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Model selection preview generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters provided")
    })
    @GetMapping("/preview")
    public ResponseEntity<Map<String, Object>> previewModelSelection(
        @Parameter(description = "File size in bytes")
        @RequestParam Long fileSize,
        
        @Parameter(description = "Estimated duration in seconds")
        @RequestParam Double duration,
        
        @Parameter(description = "Language code or 'auto'")
        @RequestParam(defaultValue = "auto") String language,
        
        @Parameter(description = "Quality preference")
        @RequestParam(defaultValue = "balanced") String quality,
        
        @Parameter(description = "User-specified model (optional)")
        @RequestParam(required = false) String userModel
    ) {
        try {
            // Create mock file metadata for preview
            // Note: This is a simplified preview - actual implementation would need a mock MultipartFile
            Map<String, Object> response = new HashMap<>();
            
            // Simple model selection logic for preview (simplified version of the actual logic)
            QualityPreference qualityPref = QualityPreference.fromString(quality);
            String selectedModel = previewModelSelection(fileSize, duration, language, qualityPref, userModel);
            
            response.put("selected_model", selectedModel);
            response.put("input_parameters", Map.of(
                "file_size_bytes", fileSize,
                "duration_seconds", duration, 
                "language", language,
                "quality_preference", qualityPref.name(),
                "user_specified_model", userModel != null ? userModel : "auto"
            ));
            response.put("selection_reason", getSelectionReason(fileSize, duration, qualityPref));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid parameters: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Simplified model selection for preview purposes.
     */
    private String previewModelSelection(Long fileSize, Double duration, String language, 
                                       QualityPreference quality, String userModel) {
        // If user specified a model, return it (unless it's "auto")
        if (userModel != null && !"auto".equals(userModel)) {
            return userModel;
        }
        
        // Simple selection logic based on file characteristics
        boolean isSmall = fileSize < 5 * 1024 * 1024; // 5MB
        boolean isShort = duration < 120.0; // 2 minutes
        boolean isLarge = fileSize > 50 * 1024 * 1024; // 50MB
        boolean isLong = duration > 1800.0; // 30 minutes
        
        if (isShort && isSmall) {
            return switch (quality) {
                case SPEED -> "tiny";
                case BALANCED -> "base"; 
                case ACCURACY, PRECISION -> "small";
            };
        }
        
        if (isLarge || isLong) {
            return switch (quality) {
                case SPEED -> "base";
                case BALANCED -> "small";
                case ACCURACY -> "medium";
                case PRECISION -> "medium"; // Cap at medium for very large files
            };
        }
        
        return switch (quality) {
            case SPEED -> "base";
            case BALANCED -> "small";
            case ACCURACY -> "medium";
            case PRECISION -> "large";
        };
    }
    
    /**
     * Get human-readable explanation for model selection.
     */
    private String getSelectionReason(Long fileSize, Double duration, QualityPreference quality) {
        boolean isSmall = fileSize < 5 * 1024 * 1024;
        boolean isShort = duration < 120.0;
        boolean isLarge = fileSize > 50 * 1024 * 1024;
        boolean isLong = duration > 1800.0;
        
        StringBuilder reason = new StringBuilder();
        reason.append("Selected based on: ");
        
        if (isShort && isSmall) {
            reason.append("small, short file - prioritizing speed");
        } else if (isLarge || isLong) {
            reason.append("large/long file - balancing accuracy with processing constraints");
        } else {
            reason.append("medium-sized file - following quality preference");
        }
        
        reason.append(" (quality preference: ").append(quality.name().toLowerCase()).append(")");
        
        return reason.toString();
    }
}

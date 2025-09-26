# Model Selection Strategy Implementation

## Overview

The Speech to Text service now implements the **Strategy Pattern** for intelligent model selection. This pattern allows for flexible and configurable selection of optimal Whisper models based on audio characteristics, quality preferences, and processing constraints.

## Architecture

### Strategy Pattern Components

1. **ModelSelectionStrategy Interface** - Defines the contract for model selection strategies
2. **Concrete Strategy Implementations**:
   - `DefaultModelSelectionStrategy` - Balanced approach considering all factors
   - `PerformanceOptimizedStrategy` - Prioritizes speed over accuracy
   - `AccuracyFocusedStrategy` - Prioritizes accuracy over speed
3. **ModelSelectionService** - Context class that manages strategies
4. **Supporting Classes**:
   - `AudioMetadata` - Encapsulates audio file characteristics
   - `QualityPreference` - Defines quality vs speed trade-offs

## Features

### Intelligent Model Selection

The system automatically selects the optimal Whisper model based on:

- **File Size** - Smaller files can use faster models efficiently
- **Estimated Duration** - Longer files may require speed optimization
- **Language Complexity** - Some languages benefit from larger models
- **Quality Preference** - User's preference for speed vs accuracy trade-off
- **Processing Constraints** - Memory and time limitations

### Quality Preferences

Four quality preference levels are supported:

| Preference | Priority | Use Case | Processing Time Multiplier |
|------------|----------|----------|---------------------------|
| `SPEED` | Fast processing | Real-time apps, quick previews | 1.0x |
| `BALANCED` | Balanced approach | General purpose, production | 2.0x |
| `ACCURACY` | High accuracy | Professional transcription | 4.0x |
| `PRECISION` | Maximum accuracy | Critical applications | 8.0x |

### Model Characteristics

| Model | Size | Speed | Accuracy | Best For |
|-------|------|-------|----------|----------|
| `tiny` | ~39 MB | ~32x realtime | Lower | Real-time, quick previews |
| `base` | ~74 MB | ~16x realtime | Good | General purpose |
| `small` | ~244 MB | ~6x realtime | Better | Production quality |
| `medium` | ~769 MB | ~2x realtime | High | Professional work |
| `large` | ~1550 MB | ~1x realtime | Highest | Critical accuracy |

## API Enhancements

### Updated Transcription Endpoint

```http
POST /api/v1/transcriptions
```

**New Parameters:**

- `model` - Now accepts "auto" for intelligent selection (default: "auto")
- `quality` - Quality preference: "speed", "balanced", "accuracy", "precision" (default: "balanced")

**Example Request:**
```bash
curl -X POST "http://localhost:8080/api/v1/transcriptions" \
  -F "file=@audio.wav" \
  -F "language=en" \
  -F "model=auto" \
  -F "quality=balanced"
```

### New Model Information Endpoints

#### Get Available Models
```http
GET /api/v1/models
```

Returns detailed information about all available Whisper models with their characteristics.

#### Get Available Strategies
```http
GET /api/v1/models/strategies
```

Returns information about available model selection strategies.

#### Get Quality Preferences
```http
GET /api/v1/models/quality-preferences
```

Returns available quality preferences with descriptions.

#### Preview Model Selection
```http
GET /api/v1/models/preview?fileSize=1048576&duration=120&language=en&quality=balanced
```

Preview which model would be selected for given parameters without creating a transcription job.

## Configuration

### Application Properties

```yaml
app:
  model-selection:
    strategy: default                    # Strategy: default, performance, accuracy
    enable-dynamic: true                 # Enable dynamic model selection
    enable-auto-selection: true          # Allow automatic model selection
    fallback-model: base                 # Fallback when selection fails
    max-processing-time-minutes: 30      # Maximum acceptable processing time
```

### Environment Variables

- `MODEL_SELECTION_STRATEGY` - Override strategy selection
- `ENABLE_DYNAMIC_SELECTION` - Enable/disable dynamic selection
- `ENABLE_AUTO_SELECTION` - Enable/disable auto-selection
- `FALLBACK_MODEL` - Default model when selection fails
- `MAX_PROCESSING_TIME` - Maximum processing time in minutes

## Strategy Implementations

### DefaultModelSelectionStrategy

**Balanced approach** that considers all factors:

- **Small, short files** → Use faster models (tiny, base, small)
- **Large or long files** → Balance accuracy with constraints
- **Complex languages** → Prefer larger models
- **Quality preference** → Adjust model size accordingly

**Selection Logic:**
```java
// For short files (< 2 min) and small size (< 5MB)
SPEED → tiny, BALANCED → base, ACCURACY → small

// For large files (> 50MB) or long duration (> 30 min)  
SPEED → base, BALANCED → small, ACCURACY → medium

// For complex languages (Chinese, Japanese, etc.)
Upgrade model by one size level when feasible
```

### PerformanceOptimizedStrategy

**Speed-focused** approach for high-throughput scenarios:

- Caps maximum model size at "small"
- Prefers "tiny" and "base" models
- Optimized for real-time processing
- Limited to files under 1 hour

### AccuracyFocusedStrategy

**Accuracy-focused** approach for critical applications:

- Prefers larger models (medium, large)
- Biases towards better models for complex languages
- Suitable for professional transcription
- Balances accuracy with practical constraints

## Usage Examples

### Basic Usage with Auto Selection

```java
// Request with auto model selection
TranscriptionUploadRequest request = new TranscriptionUploadRequest(
    "en",           // language
    false,          // not synchronous  
    "auto",         // auto model selection
    false,          // no diarization
    "balanced"      // balanced quality
);
```

### Explicit Model Selection

```java
// Request with specific model
TranscriptionUploadRequest request = new TranscriptionUploadRequest(
    "en",           // language
    false,          // not synchronous
    "medium",       // explicit model choice
    true,           // enable diarization
    "accuracy"      // high accuracy preference
);
```

### Programmatic Model Selection

```java
@Autowired
private ModelSelectionService modelSelectionService;

public void processAudio(MultipartFile file) {
    String selectedModel = modelSelectionService.selectModel(
        file,                           // audio file
        "auto",                         // user model preference
        "en",                           // language
        QualityPreference.BALANCED      // quality preference
    );
    
    // Use selectedModel for transcription
}
```

## Selection Examples

### Example 1: Small English File
- **File**: 2MB, 90 seconds, English
- **Quality**: Balanced
- **Selected**: `base` 
- **Reason**: Small file, common language, balanced preference

### Example 2: Large Chinese File  
- **File**: 75MB, 25 minutes, Chinese
- **Quality**: Accuracy
- **Selected**: `medium`
- **Reason**: Large file (capped), complex language, accuracy preference

### Example 3: Long English Podcast
- **File**: 120MB, 60 minutes, English
- **Quality**: Speed
- **Selected**: `base`
- **Reason**: Long file, speed priority, memory constraints

### Example 4: Short Japanese Audio
- **File**: 1.5MB, 45 seconds, Japanese
- **Quality**: Precision
- **Selected**: `small`
- **Reason**: Small file, complex language, high accuracy need

## Benefits

### For Users
- **Automatic Optimization** - No need to understand model trade-offs
- **Improved Performance** - Optimal model selection for each file
- **Flexible Control** - Can override with explicit model choice
- **Quality Control** - Simple quality preference selection

### For Developers
- **Extensible Design** - Easy to add new selection strategies
- **Configurable Behavior** - Extensive configuration options
- **Testable Logic** - Clean separation of concerns
- **Monitoring Support** - Built-in logging and metrics

### For Operations
- **Resource Optimization** - Efficient use of computing resources
- **Predictable Performance** - Better processing time estimates
- **Configuration Control** - Environment-specific tuning
- **Fallback Handling** - Robust error handling

## Migration Guide

### Existing API Compatibility

The implementation is **backward compatible**:

- Old requests with explicit models work unchanged
- Default model parameter changed from "base" to "auto"
- New quality parameter defaults to "balanced"

### Migration Steps

1. **Update client applications** to use "auto" for model parameter
2. **Add quality preferences** to improve selection accuracy
3. **Monitor model selections** through logs and metrics
4. **Tune configuration** based on usage patterns

### Configuration Migration

```yaml
# Old configuration
app:
  transcription:
    default-model: base

# New configuration  
app:
  model-selection:
    strategy: default
    enable-auto-selection: true
    fallback-model: base
```

## Performance Impact

### Processing Time Estimation

The strategy considers estimated processing times:

```
Tiny:   Duration × 0.03  (32x realtime)
Base:   Duration × 0.06  (16x realtime)  
Small:  Duration × 0.17  (6x realtime)
Medium: Duration × 0.50  (2x realtime)
Large:  Duration × 1.00  (1x realtime)
```

### Memory Usage

Model memory requirements:
- **tiny**: ~120MB RAM
- **base**: ~150MB RAM  
- **small**: ~400MB RAM
- **medium**: ~900MB RAM
- **large**: ~1700MB RAM

## Monitoring and Debugging

### Logging

Model selection decisions are logged:

```
INFO  c.s.a.s.DefaultModelSelectionStrategy - Selected model 'small' for audio: 
      AudioMetadata{size=5242880 bytes, duration=180.0s, language='en', complexity=1.05} 
      with preference: BALANCED
```

### Metrics

Key metrics exposed:
- `model_selection_duration_seconds` - Time spent selecting models
- `model_selected_total{model="small"}` - Counter of models selected
- `model_selection_strategy{strategy="default"}` - Strategy usage

### Health Checks

Model selection health is included in application health checks:

```json
{
  "modelSelection": {
    "status": "UP",
    "details": {
      "strategiesAvailable": 3,
      "autoSelectionEnabled": true,
      "fallbackModel": "base"
    }
  }
}
```

## Testing

### Unit Tests

Comprehensive test coverage for:
- Strategy implementations
- Audio metadata analysis  
- Quality preference parsing
- Model selection service
- Edge cases and error handling

### Integration Tests

End-to-end testing of:
- API parameter handling
- Model selection pipeline
- Configuration loading
- Strategy switching

### Performance Tests

Load testing with:
- Various file sizes and types
- Different quality preferences
- Strategy performance comparison
- Memory usage validation

## Troubleshooting

### Common Issues

1. **Model selection taking too long**
   - Check file size estimation algorithm
   - Verify strategy configuration
   - Monitor processing time limits

2. **Unexpected model selections**
   - Review audio metadata detection
   - Check quality preference mapping
   - Validate strategy selection logic

3. **Fallback model used frequently**
   - Investigate strategy failures
   - Review input validation
   - Check configuration values

### Debug Commands

```bash
# Test model selection
curl "http://localhost:8080/api/v1/models/preview?fileSize=1048576&duration=120&quality=balanced"

# Check available strategies
curl "http://localhost:8080/api/v1/models/strategies"

# View health status
curl "http://localhost:8080/actuator/health"
```

## Future Enhancements

### Planned Features

1. **Machine Learning Integration** - Use ML models to predict optimal model selection
2. **Historical Performance Tracking** - Learn from past transcription results
3. **User Preference Learning** - Adapt to individual user preferences
4. **Dynamic Strategy Loading** - Hot-swap strategies without restarts
5. **Multi-Model Ensembles** - Combine multiple models for better accuracy

### Extensibility Points

- **Custom Strategies** - Implement domain-specific selection logic
- **External Integrations** - Connect to external decision services
- **Advanced Metrics** - Add business-specific success metrics
- **A/B Testing** - Compare strategy effectiveness

The Strategy pattern implementation provides a robust, flexible, and extensible foundation for intelligent model selection in the Speech to Text service, improving both user experience and system efficiency.

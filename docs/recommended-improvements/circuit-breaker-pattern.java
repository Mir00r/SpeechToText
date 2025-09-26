// Add to build.gradle
implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.1.0'
implementation 'io.github.resilience4j:resilience4j-circuitbreaker:2.1.0'

// Configuration class
@Configuration
@EnableConfigurationProperties(CircuitBreakerConfigurationProperties.class)
public class ResilienceConfig {
    
    @Bean
    public CircuitBreaker transcriptionServiceCircuitBreaker() {
        return CircuitBreaker.ofDefaults("transcriptionService");
    }
    
    @Bean 
    public TimeLimiter transcriptionServiceTimeLimiter() {
        return TimeLimiter.ofDefaults("transcriptionService");
    }
}

// Enhanced TranscriptionServiceClient with circuit breaker
@Component
public class TranscriptionServiceClient {
    
    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;
    
    @CircuitBreaker(name = "transcriptionService", fallbackMethod = "fallbackTranscribe")
    @TimeLimiter(name = "transcriptionService")
    public CompletableFuture<String> submitTranscription(TranscriptionRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            // Original transcription service call
            return webClient.post()
                .uri("/transcribe")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofMinutes(5));
        });
    }
    
    // Fallback method for circuit breaker
    public CompletableFuture<String> fallbackTranscribe(TranscriptionRequest request, Exception ex) {
        logger.error("Transcription service unavailable, using fallback", ex);
        // Could implement queue-based retry or alternative processing
        throw new ExternalServiceException("transcription-service", "Service temporarily unavailable");
    }
}

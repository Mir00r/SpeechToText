package com.speechtotext.api.integration;

import com.speechtotext.api.dto.TranscriptionCallbackRequest;
import com.speechtotext.api.dto.TranscriptionJobResponse;
import com.speechtotext.api.model.JobEntity;
import com.speechtotext.api.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.speechtotext.api.infra.s3.S3ClientAdapter;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

package com.speechtotext.api.integration;

import com.speechtotext.api.dto.TranscriptionCallbackRequest;
import com.speechtotext.api.dto.TranscriptionJobResponse;
import com.speechtotext.api.model.JobEntity;
import com.speechtotext.api.repository.JobRepository;
import com.speechtotext.api.infra.s3.S3ClientAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration tests for the complete transcription flow.
 * Uses Testcontainers for realistic database testing with mocked S3.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class TranscriptionIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("speechtotext_test")
            .withUsername("test")
            .withPassword("test");

    @MockBean
    private S3ClientAdapter s3ClientAdapter;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Database configuration
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Mock S3 configuration
        registry.add("app.s3.endpoint", () -> "http://localhost:9000");
        registry.add("app.s3.access-key", () -> "testuser");
        registry.add("app.s3.secret-key", () -> "testpass123");
        registry.add("app.s3.bucket-name", () -> "test-bucket");

        // Test configuration
        registry.add("app.transcription.sync-threshold-seconds", () -> 30);
        registry.add("app.transcription.sync-timeout-seconds", () -> 60);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JobRepository jobRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        jobRepository.deleteAll();
        
        // Mock S3 upload
        when(s3ClientAdapter.uploadFile(any(), any())).thenReturn("s3://test-bucket/test-file.wav");
    }

    @Test
    void shouldCreateAsyncTranscriptionJob() {
        // Arrange
        byte[] audioContent = new byte[2_000_000]; // 2MB - large file for async processing
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "test_long.wav", 
            "audio/wav", 
            audioContent
        );

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", file.getResource());
        parts.add("language", "en");
        parts.add("model", "base");
        parts.add("sync", "false");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Act - Submit transcription job
        ResponseEntity<TranscriptionJobResponse> response = restTemplate.exchange(
            baseUrl + "/api/v1/transcriptions",
            HttpMethod.POST,
            new HttpEntity<>(parts, headers),
            TranscriptionJobResponse.class
        );

        // Assert - Job created
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        TranscriptionJobResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isIn(
            JobEntity.JobStatus.PENDING, 
            JobEntity.JobStatus.PROCESSING
        );

        UUID jobId = body.id();
        assertThat(jobId).isNotNull();

        // Verify job in database
        JobEntity job = jobRepository.findById(jobId).orElse(null);
        assertThat(job).isNotNull();
        assertThat(job.getOriginalFilename()).isEqualTo("test_long.wav");
        assertThat(job.getModel()).isEqualTo("base");
        assertThat(job.getLanguage()).isEqualTo("en");
    }

    @Test
    void shouldHandleCallback() {
        // Arrange - Create a job first
        JobEntity job = new JobEntity("test.wav", "original_test.wav", "s3://bucket/test.wav");
        job.setStatus(JobEntity.JobStatus.PROCESSING);
        job = jobRepository.save(job);

        TranscriptionCallbackRequest callback = new TranscriptionCallbackRequest(
            "completed",
            "Hello world, this is a test recording.",
            "s3://bucket/transcript.txt",
            null,
            1500L,
            30.0,
            null, // segments
            null, // speaker segments
            null  // metadata
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/internal/v1/transcriptions/" + job.getId() + "/callback",
            HttpMethod.POST,
            new HttpEntity<>(callback, headers),
            String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Callback processed successfully");

        // Verify job updated in database
        JobEntity updatedJob = jobRepository.findById(job.getId()).orElse(null);
        assertThat(updatedJob).isNotNull();
        assertThat(updatedJob.getStatus()).isEqualTo(JobEntity.JobStatus.COMPLETED);
        assertThat(updatedJob.getTranscriptText()).isEqualTo("Hello world, this is a test recording.");
    }

    @Test
    void shouldRejectInvalidFile() {
        // Arrange
        MockMultipartFile invalidFile = new MockMultipartFile(
            "file", 
            "test.txt", 
            "text/plain", 
            "This is not an audio file".getBytes()
        );

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", invalidFile.getResource());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/api/v1/transcriptions",
            HttpMethod.POST,
            new HttpEntity<>(parts, headers),
            String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("not supported");
    }

    @Test
    void shouldRejectCallbackForNonExistentJob() {
        // Arrange
        UUID nonExistentJobId = UUID.randomUUID();
        TranscriptionCallbackRequest callback = new TranscriptionCallbackRequest(
            "completed",
            "Some transcript",
            null, null, null, null, null, null, null
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/internal/v1/transcriptions/" + nonExistentJobId + "/callback",
            HttpMethod.POST,
            new HttpEntity<>(callback, headers),
            String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Job not found");
    }
}

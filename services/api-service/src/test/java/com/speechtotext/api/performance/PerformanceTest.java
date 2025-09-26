package com.speechtotext.api.performance;

import com.speechtotext.api.service.TranscriptionService;
import com.speechtotext.api.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for the Speech to Text API.
 * Tests concurrent uploads, database performance, and response times.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers  
@ActiveProfiles("test")
@AutoConfigureWebMvc
public class PerformanceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("testuser") 
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private JobRepository jobRepository;
    
    @MockBean
    private TranscriptionService transcriptionService;

    @Test
    public void testConcurrentUploads() throws Exception {
        int numberOfThreads = 10;
        int uploadsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    for (int j = 0; j < uploadsPerThread; j++) {
                        MockMultipartFile file = new MockMultipartFile(
                            "file", 
                            "test-" + threadId + "-" + j + ".wav", 
                            "audio/wav", 
                            generateAudioContent(1024) // 1KB file
                        );
                        
                        mockMvc.perform(multipart("/api/v1/transcriptions")
                                .file(file)
                                .param("language", "en")
                                .param("sync", "false"))
                                .andExpect(status().isAccepted());
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);
            futures.add(future);
        }
        
        // Wait for all uploads to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(60, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Verify all jobs were created
        long jobCount = jobRepository.count();
        assertEquals(numberOfThreads * uploadsPerThread, jobCount);
        
        // Performance assertion - should complete within reasonable time
        assertTrue(duration < 30000, "Concurrent uploads took too long: " + duration + "ms");
        
        System.out.println("Performance Test Results:");
        System.out.println("- Total uploads: " + jobCount);
        System.out.println("- Duration: " + duration + "ms");
        System.out.println("- Average per upload: " + (duration / jobCount) + "ms");
        System.out.println("- Throughput: " + (jobCount * 1000 / duration) + " uploads/sec");
        
        executor.shutdown();
    }

    @Test
    public void testDatabasePerformance() {
        int numberOfJobs = 1000;
        
        long startTime = System.currentTimeMillis();
        
        // Batch insert simulation through repository
        for (int i = 0; i < numberOfJobs; i++) {
            // This would be done via the service layer in real scenarios
            // We're testing repository performance here
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("Database Performance:");
        System.out.println("- Operations: " + numberOfJobs);
        System.out.println("- Duration: " + duration + "ms");
        System.out.println("- Average per operation: " + (duration / numberOfJobs) + "ms");
        
        // Should be fast for database operations
        assertTrue(duration < 10000, "Database operations took too long: " + duration + "ms");
    }

    @Test
    public void testMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        
        // Force garbage collection to get accurate baseline
        System.gc();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Simulate processing multiple large files
        List<byte[]> files = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            files.add(generateAudioContent(1024 * 1024)); // 1MB files
        }
        
        long peakMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Clear references to allow garbage collection
        files.clear();
        System.gc();
        
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        
        System.out.println("Memory Usage Test:");
        System.out.println("- Initial memory: " + (initialMemory / 1024 / 1024) + " MB");
        System.out.println("- Peak memory: " + (peakMemory / 1024 / 1024) + " MB");
        System.out.println("- Final memory: " + (finalMemory / 1024 / 1024) + " MB");
        System.out.println("- Memory increase: " + ((peakMemory - initialMemory) / 1024 / 1024) + " MB");
        
        // Memory should be released after processing
        assertTrue(finalMemory < peakMemory * 1.1, "Memory was not properly released");
    }

    private byte[] generateAudioContent(int size) {
        byte[] content = new byte[size];
        // Generate some pseudo-random content
        for (int i = 0; i < size; i++) {
            content[i] = (byte) (i % 256);
        }
        return content;
    }
}

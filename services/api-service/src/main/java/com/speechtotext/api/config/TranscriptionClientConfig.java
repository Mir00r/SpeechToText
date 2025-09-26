package com.speechtotext.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.time.Duration;

@Configuration
public class TranscriptionClientConfig {

    @Value("${transcription.service.base-url:http://transcription-service:8001}")
    private String transcriptionServiceBaseUrl;

    @Value("${transcription.service.timeout:300}")
    private int timeoutSeconds;

    @Bean
    public RestTemplate transcriptionRestTemplate(RestTemplateBuilder builder) {
        return builder
                .rootUri(transcriptionServiceBaseUrl)
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(timeoutSeconds))
                .requestFactory(HttpComponentsClientHttpRequestFactory::new)
                .build();
    }
}

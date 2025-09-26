package com.speechtotext.api.trace;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration class for registering request tracing components.
 */
@Configuration
public class TracingConfiguration implements WebMvcConfigurer {
    
    private final RequestTracingInterceptor requestTracingInterceptor;
    
    @Autowired
    public TracingConfiguration(RequestTracingInterceptor requestTracingInterceptor) {
        this.requestTracingInterceptor = requestTracingInterceptor;
    }
    
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(requestTracingInterceptor)
                .addPathPatterns("/api/**", "/internal/**")  // Only apply to API endpoints
                .excludePathPatterns("/actuator/**", "/swagger-ui/**", "/api-docs/**"); // Exclude health/docs
    }
}

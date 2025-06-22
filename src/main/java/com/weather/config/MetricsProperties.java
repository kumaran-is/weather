package com.weather.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Configuration properties for metrics collection.
 * Allows configuring which API endpoints should be included in custom metrics.
 */
@Data
@Component
@ConfigurationProperties(prefix = "weather.metrics")
public class MetricsProperties {
    
    /**
     * List of API path prefixes to include in custom metrics collection.
     * Supports multiple values for future extensibility.
     * Default: ["/api/v1/"]
     */
    private List<String> includedApiPaths = List.of("/api/v1/");
    
    /**
     * Whether to enable custom reactive metrics collection.
     * Default: true
     */
    private boolean enabled = true;
    
    /**
     * Whether to include detailed URI and method tags in metrics.
     * Default: true
     */
    private boolean includeDetailedTags = true;
    
    /**
     * Check if the given request path should be included in metrics collection.
     */
    public boolean shouldIncludeInMetrics(String requestPath) {
        if (!enabled || requestPath == null) {
            return false;
        }
        
        return includedApiPaths.stream()
                .anyMatch(requestPath::startsWith);
    }
}
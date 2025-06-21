package com.weather.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to filter out management endpoint metrics from built-in Spring metrics.
 * Only includes configurable business API endpoints in http.server.requests metrics.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MetricsFilterConfig {
    
    private final MetricsProperties metricsProperties;

    /**
     * Custom meter registry filter to exclude non-API endpoints from http.server.requests metrics.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            registry.config()
                    .meterFilter(MeterFilter.deny(id -> {
                        // Filter http.server.requests metrics based on configurable API paths
                        if ("http.server.requests".equals(id.getName())) {
                            String uri = id.getTag("uri");
                            if (uri != null) {
                                // Deny if it's a management/actuator endpoint or unknown
                                if (uri.startsWith("/management/") || 
                                    uri.startsWith("/actuator/") ||
                                    "UNKNOWN".equals(uri)) {
                                    log.debug("Filtering out management endpoint from metrics: {}", uri);
                                    return true; // Deny this metric
                                }
                                
                                // Deny if it doesn't match any configured API paths
                                if (!metricsProperties.shouldIncludeInMetrics(uri)) {
                                    log.debug("Filtering out non-API endpoint from metrics: {}", uri);
                                    return true; // Deny this metric
                                }
                            }
                        }
                        return false; // Allow all other metrics
                    }));
            
            log.info("Configured metrics filter for API paths: {}", metricsProperties.getIncludedApiPaths());
        };
    }
}
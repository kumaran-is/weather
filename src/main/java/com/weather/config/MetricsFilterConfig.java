package com.weather.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to filter out management endpoint metrics from built-in Spring metrics.
 * Only includes business API endpoints under /api/v1/ in http.server.requests metrics.
 */
@Slf4j
@Configuration
public class MetricsFilterConfig {

    /**
     * Custom meter registry filter to exclude management endpoints from http.server.requests metrics.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            registry.config()
                    .meterFilter(MeterFilter.deny(id -> {
                        // Filter out management endpoints from http.server.requests metrics
                        if ("http.server.requests".equals(id.getName())) {
                            String uri = id.getTag("uri");
                            if (uri != null && (uri.startsWith("/management/") || 
                                              uri.startsWith("/actuator/") ||
                                              "UNKNOWN".equals(uri))) {
                                log.debug("Filtering out management endpoint from metrics: {}", uri);
                                return true; // Deny this metric
                            }
                        }
                        return false; // Allow all other metrics
                    }));
            
            log.info("Configured metrics filter to exclude management endpoints from http.server.requests");
        };
    }
}
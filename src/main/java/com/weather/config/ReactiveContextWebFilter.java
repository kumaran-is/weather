package com.weather.config;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Web filter that automatically adds context propagation to all reactive requests.
 * Ensures correlation IDs, timing, and user context flow through reactive chains.
 * Uses Spring Boot 3.4.x structured logging with ECS format for better observability.
 */
@Component
@RequiredArgsConstructor
public class ReactiveContextWebFilter implements WebFilter {
    
    private static final Logger log = LogManager.getLogger(ReactiveContextWebFilter.class);
    
    private final ReactiveContextConfig.ReactiveContextPropagation contextPropagation;
    private final ReactiveMetricsConfig.ReactiveMetricsCollector metricsCollector;
    private final MetricsProperties metricsProperties;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestPath = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        
        // Use configurable API paths to determine if metrics should be collected
        boolean isApiEndpoint = metricsProperties.shouldIncludeInMetrics(requestPath);
        Long startTime = isApiEndpoint ? System.nanoTime() : null;
        
        // Generate correlation ID for this request
        String correlationId = contextPropagation.generateCorrelationId();
        String userId = "anonymous";
        
        // Capture the initial ThreadContext state
        final java.util.Map<String, String> contextData = java.util.Map.of(
            "correlation_id", correlationId,
            "user_id", userId,
            "request_path", requestPath,
            "request_method", method
        );
        
        // Set ThreadContext immediately for this request
        ThreadContext.putAll(contextData);
        
        log.debug("Processing request {} {}", method, requestPath);
        
        return chain.filter(exchange)
                .contextWrite(context -> {
                    // Create enriched context with all the cross-cutting concern data
                    return context
                            .put(ReactiveContextConfig.CORRELATION_ID_KEY, correlationId)
                            .put(ReactiveContextConfig.REQUEST_START_TIME_KEY, System.currentTimeMillis())
                            .put(ReactiveContextConfig.USER_CONTEXT_KEY, ReactiveContextConfig.UserContext.anonymous())
                            .put("request.path", requestPath)
                            .put("request.method", method)
                            .put("threadContext", contextData); // Store context data for propagation
                })
                .doOnEach(signal -> {
                    // Restore ThreadContext on every signal emission across different threads
                    ThreadContext.putAll(contextData);
                })
                .doOnSuccess(unused -> {
                    // Ensure ThreadContext is set for logging
                    ThreadContext.putAll(contextData);
                    if (isApiEndpoint && startTime != null) {
                        metricsCollector.stopTimer(startTime, "api_request", "success", requestPath, method);
                    }
                    log.debug("Request {} {} completed successfully", method, requestPath);
                })
                .doOnError(error -> {
                    // Ensure ThreadContext is set for logging
                    ThreadContext.putAll(contextData);
                    if (isApiEndpoint && startTime != null) {
                        metricsCollector.stopTimer(startTime, "api_request", "error", requestPath, method);
                    }
                    log.error("Request {} {} failed: {}", method, requestPath, error.getMessage(), error);
                })
                .doFinally(signalType -> {
                    // Clear ThreadContext when the reactive chain completes
                    ThreadContext.clearAll();
                });
    }
}
package com.weather.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * Web filter that automatically adds context propagation to all reactive requests.
 * Ensures correlation IDs, timing, and user context flow through reactive chains.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReactiveContextWebFilter implements WebFilter {
    
    private final ReactiveContextConfig.ReactiveContextPropagation contextPropagation;
    private final ReactiveMetricsConfig.ReactiveMetricsCollector metricsCollector;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestPath = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        
        // Automatically collect metrics for all reactive requests
        long startTime = System.nanoTime();
        
        return chain.filter(exchange)
                .contextWrite(context -> enrichContext(context, requestPath, method))
                .doOnSuccess(unused -> {
                    // Record request timing automatically
                    metricsCollector.stopTimer(startTime, "http_request", "success");
                    logRequestCompletion(requestPath, method);
                })
                .doOnError(error -> {
                    // Record error timing automatically  
                    metricsCollector.stopTimer(startTime, "http_request", "error");
                    logRequestError(requestPath, method, error);
                });
    }
    
    /**
     * Enrich the reactive context with cross-cutting concern data.
     */
    private Context enrichContext(Context context, String requestPath, String method) {
        // Use ReactiveContextPropagation utility methods for proper context enrichment
        // Using putAll(ContextView) with readOnly() to avoid deprecated putAll(Context)
        Context enrichedContext = context
                .putAll(contextPropagation.withCorrelationId().readOnly())
                .putAll(contextPropagation.withRequestTiming().readOnly())
                .putAll(contextPropagation.withUserContext("anonymous", "anonymous-user").readOnly());
        
        // Add request metadata for tracing
        enrichedContext = enrichedContext.put("request.path", requestPath);
        enrichedContext = enrichedContext.put("request.method", method);
        
        log.debug("Enriched reactive context for {} {} with correlation ID and timing", method, requestPath);
        return enrichedContext;
    }
    
    private void logRequestCompletion(String requestPath, String method) {
        log.debug("Request {} {} completed successfully with reactive context propagation", method, requestPath);
    }
    
    private void logRequestError(String requestPath, String method, Throwable error) {
        log.error("Request {} {} failed with reactive context: {}", method, requestPath, error.getMessage());
    }
}
package com.weather.util;

import com.weather.config.ReactiveContextConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.util.context.ContextView;

import java.util.function.Consumer;

/**
 * Utility class for logging with reactive context information.
 * Provides methods to extract context values and populate MDC for structured logging.
 */
public class ReactiveContextLogger {

    private static final Logger log = LogManager.getLogger(ReactiveContextLogger.class);

    /**
     * Execute a logging operation with reactive context values in MDC.
     */
    public static <T> Mono<T> withContextLogging(Mono<T> publisher, Consumer<ContextView> logOperation) {
        return publisher
                .doOnEach(signal -> {
                    if (!signal.isOnComplete() && !signal.isOnError()) {
                        return;
                    }
                    try {
                        populateMDCFromContext(signal.getContextView());
                        logOperation.accept(signal.getContextView());
                    } finally {
                        clearMDC();
                    }
                });
    }

    /**
     * Execute a simple logging operation with reactive context.
     */
    public static <T> Mono<T> withContextLogging(Mono<T> publisher, String message) {
        return withContextLogging(publisher, context -> {
            String correlationId = getCorrelationId(context);
            String userId = getUserId(context);
            log.info("{} [correlationId: {}] [userId: {}]", message, correlationId, userId);
        });
    }

    /**
     * Populate Log4j2 MDC with reactive context values.
     */
    public static void populateMDCFromContext(ContextView context) {
        // Clear any existing MDC values first
        ThreadContext.clearAll();
        
        // Extract and set correlation ID
        context.getOrEmpty(ReactiveContextConfig.CORRELATION_ID_KEY)
                .ifPresent(correlationId -> ThreadContext.put("correlationId", correlationId.toString()));

        // Extract and set user context
        context.getOrEmpty(ReactiveContextConfig.USER_CONTEXT_KEY)
                .ifPresent(userContext -> {
                    if (userContext instanceof ReactiveContextConfig.UserContext user) {
                        ThreadContext.put("userId", user.userId());
                        ThreadContext.put("username", user.username());
                    }
                });

        // Extract and set request timing if available
        context.getOrEmpty(ReactiveContextConfig.REQUEST_START_TIME_KEY)
                .ifPresent(startTime -> ThreadContext.put("requestStartTime", startTime.toString()));

        // Extract and set request metadata if available
        context.getOrEmpty("request.path")
                .ifPresent(path -> ThreadContext.put("requestPath", path.toString()));
        
        context.getOrEmpty("request.method")
                .ifPresent(method -> ThreadContext.put("requestMethod", method.toString()));
    }

    /**
     * Clear MDC after logging operation.
     */
    public static void clearMDC() {
        ThreadContext.clearAll();
    }

    /**
     * Get correlation ID from context with fallback.
     */
    public static String getCorrelationId(ContextView context) {
        return context.getOrEmpty(ReactiveContextConfig.CORRELATION_ID_KEY)
                .map(Object::toString)
                .orElse("unknown");
    }

    /**
     * Get user ID from context with fallback.
     */
    public static String getUserId(ContextView context) {
        return context.getOrEmpty(ReactiveContextConfig.USER_CONTEXT_KEY)
                .filter(userContext -> userContext instanceof ReactiveContextConfig.UserContext)
                .map(userContext -> ((ReactiveContextConfig.UserContext) userContext).userId())
                .orElse("anonymous");
    }

    /**
     * Get request duration from context with fallback.
     */
    public static String getRequestDuration(ContextView context) {
        return context.getOrEmpty(ReactiveContextConfig.REQUEST_START_TIME_KEY)
                .map(startTime -> {
                    long duration = System.currentTimeMillis() - (Long) startTime;
                    return duration + "ms";
                })
                .orElse("unknown");
    }

    /**
     * Log with automatic context extraction.
     */
    public static <T> Consumer<Signal<T>> logOnNext(String message) {
        return signal -> {
            if (signal.isOnNext()) {
                try {
                    populateMDCFromContext(signal.getContextView());
                    log.info("{}", message);
                } finally {
                    clearMDC();
                }
            }
        };
    }

    /**
     * Log with automatic context extraction for completion.
     */
    public static <T> Consumer<Signal<T>> logOnComplete(String message) {
        return signal -> {
            if (signal.isOnComplete()) {
                try {
                    populateMDCFromContext(signal.getContextView());
                    String duration = getRequestDuration(signal.getContextView());
                    log.info("{} (duration: {})", message, duration);
                } finally {
                    clearMDC();
                }
            }
        };
    }

    /**
     * Log with automatic context extraction for errors.
     */
    public static <T> Consumer<Signal<T>> logOnError(String message) {
        return signal -> {
            if (signal.isOnError()) {
                try {
                    populateMDCFromContext(signal.getContextView());
                    log.error("{}: {}", message, signal.getThrowable().getMessage(), signal.getThrowable());
                } finally {
                    clearMDC();
                }
            }
        };
    }
}
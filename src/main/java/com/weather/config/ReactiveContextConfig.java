package com.weather.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Signal;
import reactor.util.context.Context;

import java.util.Optional;
import java.util.UUID;

/**
 * Configuration for reactive context propagation including tracing, security, and correlation IDs.
 * Enables proper context handling across reactive streams for cross-cutting concerns.
 */
@Configuration
public class ReactiveContextConfig {
    
    private static final Logger log = LogManager.getLogger(ReactiveContextConfig.class);
    
    public static final String CORRELATION_ID_KEY = "correlationId";
    public static final String REQUEST_START_TIME_KEY = "requestStartTime";
    public static final String USER_CONTEXT_KEY = "userContext";
    
    /**
     * Enable automatic context propagation for reactive operators.
     * This ensures context flows through reactive chains automatically.
     */
    @Bean
    public ReactiveContextPropagation reactiveContextPropagation() {
        // Enable automatic context propagation
        Hooks.enableAutomaticContextPropagation();
        
        log.info("Enabled automatic reactive context propagation with ThreadContext population");
        
        return new ReactiveContextPropagation();
    }
    
    /**
     * Helper method to populate ThreadContext from a Reactor signal
     */
    private static void populateThreadContextFromSignal(Signal<?> signal) {
        signal.getContextView().getOrEmpty(CORRELATION_ID_KEY)
                .ifPresent(corrId -> org.apache.logging.log4j.ThreadContext.put("correlation_id", corrId.toString()));
        signal.getContextView().getOrEmpty("request.path")
                .ifPresent(path -> org.apache.logging.log4j.ThreadContext.put("request_path", path.toString()));
        signal.getContextView().getOrEmpty("request.method")
                .ifPresent(method -> org.apache.logging.log4j.ThreadContext.put("request_method", method.toString()));
        signal.getContextView().getOrEmpty(USER_CONTEXT_KEY)
                .ifPresent(userCtx -> {
                    if (userCtx instanceof UserContext user) {
                        org.apache.logging.log4j.ThreadContext.put("user_id", user.userId());
                    }
                });
    }
    
    /**
     * Utility methods for context management in reactive streams.
     */
    public static class ReactiveContextPropagation {
        
        /**
         * Create a context with correlation ID for request tracking.
         */
        public Context withCorrelationId(String correlationId) {
            return Context.of(CORRELATION_ID_KEY, correlationId);
        }
        
        /**
         * Create a context with auto-generated correlation ID.
         */
        public Context withCorrelationId() {
            return withCorrelationId(generateCorrelationId());
        }
        
        /**
         * Create a context with request timing for performance monitoring.
         */
        public Context withRequestTiming() {
            return Context.of(REQUEST_START_TIME_KEY, System.currentTimeMillis());
        }
        
        /**
         * Create a context with user information for security tracking.
         */
        public Context withUserContext(String userId, String username) {
            UserContext userContext = new UserContext(userId, username);
            return Context.of(USER_CONTEXT_KEY, userContext);
        }
        
        /**
         * Extract correlation ID from context.
         */
        public Optional<String> getCorrelationId(Context context) {
            return context.getOrEmpty(CORRELATION_ID_KEY);
        }
        
        /**
         * Extract request start time from context.
         */
        public Optional<Long> getRequestStartTime(Context context) {
            return context.getOrEmpty(REQUEST_START_TIME_KEY);
        }
        
        /**
         * Extract user context from reactive context.
         */
        public Optional<UserContext> getUserContext(Context context) {
            return context.getOrEmpty(USER_CONTEXT_KEY);
        }
        
        /**
         * Generate a unique correlation ID for request tracking.
         */
        public String generateCorrelationId() {
            return UUID.randomUUID().toString().substring(0, 8);
        }
    }
    
    /**
     * User context for security and auditing purposes.
     */
    public record UserContext(String userId, String username) {
        
        public static UserContext anonymous() {
            return new UserContext("anonymous", "anonymous");
        }
        
        public boolean isAuthenticated() {
            return !"anonymous".equals(userId);
        }
    }
}
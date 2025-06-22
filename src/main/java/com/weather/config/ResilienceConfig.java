package com.weather.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnFailureEvent;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnSuccessEvent;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Resilience configuration that provides registries with event listeners.
 * All resilience configurations are defined in application.yml.
 * This class only handles registration and event listening for weather service operations.
 */
@Configuration
public class ResilienceConfig {
    
    private static final Logger log = LogManager.getLogger(ResilienceConfig.class);

    private void attachCircuitBreakerListeners(CircuitBreaker circuitBreaker) {
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> log.info("CircuitBreaker '{}' state changed from {} to {}",
                circuitBreaker.getName(), event.getStateTransition().getFromState(), event.getStateTransition().getToState()))
            .onCallNotPermitted(event -> log.warn("CircuitBreaker '{}' call not permitted as it is in state: {}",
                circuitBreaker.getName(), circuitBreaker.getState()))
            .onError(event -> log.warn("CircuitBreaker '{}' recorded an error: {}, duration: {}ms",
                circuitBreaker.getName(), event.getThrowable().toString(), event.getElapsedDuration().toMillis()))
            .onSuccess(event -> log.debug("CircuitBreaker '{}' recorded a success, duration: {}ms",
                circuitBreaker.getName(), event.getElapsedDuration().toMillis()));
    }

    private void attachRetryListeners(Retry retry) {
        retry.getEventPublisher()
            .onRetry(event -> log.warn("Retry '{}', attempt #{}, last exception: {}, next attempt in {}ms",
                retry.getName(), event.getNumberOfRetryAttempts(), event.getLastThrowable().toString(), event.getWaitInterval().toMillis()))
            .onSuccess(event -> log.info("Retry '{}' succeeded after {} attempts.",
                retry.getName(), event.getNumberOfRetryAttempts()))
            .onError(event -> log.error("Retry '{}' failed after {} attempts, last exception: {}",
                retry.getName(), event.getNumberOfRetryAttempts(), event.getLastThrowable().toString()))
            .onIgnoredError(event -> log.warn("Retry '{}' ignored error for attempt #{}: {}",
                retry.getName(), event.getNumberOfRetryAttempts(), event.getLastThrowable().toString()));
    }

    private void attachRateLimiterListeners(RateLimiter rateLimiter) {
        rateLimiter.getEventPublisher()
            .onSuccess((RateLimiterOnSuccessEvent event) -> log.debug("RateLimiter '{}' permit acquired successfully. Event Type: {}", rateLimiter.getName(), event.getEventType()))
            .onFailure((RateLimiterOnFailureEvent event) -> log.warn("RateLimiter '{}' failed to acquire permit. Event Type: {}", rateLimiter.getName(), event.getEventType()));
    }

    private void attachTimeLimiterListeners(TimeLimiter timeLimiter) {
        timeLimiter.getEventPublisher()
            .onTimeout(event -> log.warn("TimeLimiter '{}' call timed out after {}ms",
                timeLimiter.getName(), timeLimiter.getTimeLimiterConfig().getTimeoutDuration().toMillis()))
            .onSuccess(event -> log.debug("TimeLimiter '{}' call succeeded.", timeLimiter.getName()))
            .onError(event -> log.warn("TimeLimiter '{}' call failed with error: {}",
                timeLimiter.getName(), event.getThrowable().toString()));
    }

    private void attachBulkheadListeners(Bulkhead bulkhead) {
        bulkhead.getEventPublisher()
            .onCallPermitted(event -> log.debug("Bulkhead '{}' call permitted. Available concurrent calls: {}",
                bulkhead.getName(), bulkhead.getMetrics().getAvailableConcurrentCalls()))
            .onCallRejected(event -> log.warn("Bulkhead '{}' call rejected. Available concurrent calls: {}",
                bulkhead.getName(), bulkhead.getMetrics().getAvailableConcurrentCalls()))
            .onCallFinished(event -> log.debug("Bulkhead '{}' call finished. Available concurrent calls: {}",
                bulkhead.getName(), bulkhead.getMetrics().getAvailableConcurrentCalls()));
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        registry.getEventPublisher()
            .onEntryAdded(event -> {
                log.info("CircuitBreaker config added: {}", event.getAddedEntry().getName());
                attachCircuitBreakerListeners(event.getAddedEntry());
            })
            .onEntryRemoved(event -> log.info("CircuitBreaker config removed: {}", event.getRemovedEntry().getName()))
            .onEntryReplaced(event -> {
                 log.info("CircuitBreaker config replaced: old='{}', new='{}'",
                    event.getOldEntry().getName(), event.getNewEntry().getName());
                 attachCircuitBreakerListeners(event.getNewEntry());
            });
        log.info("Default CircuitBreakerRegistry created; operational event listeners will be attached to each CB instance.");
        return registry;
    }

    @Bean
    public RetryRegistry retryRegistry() {
        RetryRegistry registry = RetryRegistry.ofDefaults();
        registry.getEventPublisher()
            .onEntryAdded(event -> {
                log.info("Retry config added: {}", event.getAddedEntry().getName());
                attachRetryListeners(event.getAddedEntry());
            })
            .onEntryRemoved(event -> log.info("Retry config removed: {}", event.getRemovedEntry().getName()))
            .onEntryReplaced(event -> {
                log.info("Retry config replaced: old='{}', new='{}'",
                    event.getOldEntry().getName(), event.getNewEntry().getName());
                attachRetryListeners(event.getNewEntry());
            });
        log.info("Default RetryRegistry created; operational event listeners will be attached to each Retry instance.");
        return registry;
    }

    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterRegistry registry = RateLimiterRegistry.ofDefaults();
        registry.getEventPublisher()
            .onEntryAdded(event -> {
                log.info("RateLimiter config added: {}", event.getAddedEntry().getName());
                attachRateLimiterListeners(event.getAddedEntry());
            })
            .onEntryRemoved(event -> log.info("RateLimiter config removed: {}", event.getRemovedEntry().getName()))
            .onEntryReplaced(event -> {
                 log.info("RateLimiter config replaced: old='{}', new='{}'",
                    event.getOldEntry().getName(), event.getNewEntry().getName());
                attachRateLimiterListeners(event.getNewEntry());
            });
        log.info("Default RateLimiterRegistry created; operational event listeners will be attached to each RateLimiter instance.");
        return registry;
    }

    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterRegistry registry = TimeLimiterRegistry.ofDefaults();
        registry.getEventPublisher()
            .onEntryAdded(event -> {
                log.info("TimeLimiter config added: {}", event.getAddedEntry().getName());
                attachTimeLimiterListeners(event.getAddedEntry());
            })
            .onEntryRemoved(event -> log.info("TimeLimiter config removed: {}", event.getRemovedEntry().getName()))
            .onEntryReplaced(event -> {
                log.info("TimeLimiter config replaced: old='{}', new='{}'",
                    event.getOldEntry().getName(), event.getNewEntry().getName());
                attachTimeLimiterListeners(event.getNewEntry());
            });
        log.info("Default TimeLimiterRegistry created; operational event listeners will be attached to each TimeLimiter instance.");
        return registry;
    }

    @Bean
    public BulkheadRegistry bulkheadRegistry() {
        BulkheadRegistry registry = BulkheadRegistry.ofDefaults();
        registry.getEventPublisher()
            .onEntryAdded(event -> {
                log.info("Bulkhead config added: {}", event.getAddedEntry().getName());
                attachBulkheadListeners(event.getAddedEntry());
            })
            .onEntryRemoved(event -> log.info("Bulkhead config removed: {}", event.getRemovedEntry().getName()))
            .onEntryReplaced(event -> {
                log.info("Bulkhead config replaced: old='{}', new='{}'",
                    event.getOldEntry().getName(), event.getNewEntry().getName());
                attachBulkheadListeners(event.getNewEntry());
            });
        log.info("Default BulkheadRegistry created; operational event listeners will be attached to each Bulkhead instance.");
        return registry;
    }
}
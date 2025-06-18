package com.weather.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResilienceHealthIndicator implements ReactiveHealthIndicator {
    
    private final CircuitBreaker weatherServiceCircuitBreaker;
    private final Retry weatherServiceRetry;
    private final RateLimiter weatherServiceRateLimiter;
    private final TimeLimiter weatherServiceTimeLimiter;
    
    @Override
    public Mono<Health> health() {
        return Mono.fromCallable(this::checkResilienceComponents)
                .onErrorResume(this::handleError);
    }
    
    private Health checkResilienceComponents() {
        Health.Builder builder = Health.up();
        
        // Circuit Breaker status
        CircuitBreaker.State circuitBreakerState = weatherServiceCircuitBreaker.getState();
        CircuitBreaker.Metrics circuitBreakerMetrics = weatherServiceCircuitBreaker.getMetrics();
        
        builder.withDetail("circuitBreaker", Health.up()
                .withDetail("state", circuitBreakerState.name())
                .withDetail("failureRate", circuitBreakerMetrics.getFailureRate())
                .withDetail("numberOfBufferedCalls", circuitBreakerMetrics.getNumberOfBufferedCalls())
                .withDetail("numberOfFailedCalls", circuitBreakerMetrics.getNumberOfFailedCalls())
                .withDetail("numberOfSuccessfulCalls", circuitBreakerMetrics.getNumberOfSuccessfulCalls())
                .build());
        
        // Rate Limiter status
        RateLimiter.Metrics rateLimiterMetrics = weatherServiceRateLimiter.getMetrics();
        
        builder.withDetail("rateLimiter", Health.up()
                .withDetail("availablePermissions", rateLimiterMetrics.getAvailablePermissions())
                .withDetail("numberOfWaitingThreads", rateLimiterMetrics.getNumberOfWaitingThreads())
                .build());
        
        // Retry status
        Retry.Metrics retryMetrics = weatherServiceRetry.getMetrics();
        
        builder.withDetail("retry", Health.up()
                .withDetail("numberOfSuccessfulCallsWithoutRetryAttempt", 
                        retryMetrics.getNumberOfSuccessfulCallsWithoutRetryAttempt())
                .withDetail("numberOfSuccessfulCallsWithRetryAttempt", 
                        retryMetrics.getNumberOfSuccessfulCallsWithRetryAttempt())
                .withDetail("numberOfFailedCallsWithoutRetryAttempt", 
                        retryMetrics.getNumberOfFailedCallsWithoutRetryAttempt())
                .withDetail("numberOfFailedCallsWithRetryAttempt", 
                        retryMetrics.getNumberOfFailedCallsWithRetryAttempt())
                .build());
        
        // Time Limiter status
        TimeLimiter.Metrics timeLimiterMetrics = weatherServiceTimeLimiter.getMetrics();
        
        builder.withDetail("timeLimiter", Health.up()
                .withDetail("numberOfSuccessfulCalls", timeLimiterMetrics.getNumberOfSuccessfulCalls())
                .withDetail("numberOfFailedCalls", timeLimiterMetrics.getNumberOfFailedCalls())
                .withDetail("numberOfTimeoutCalls", timeLimiterMetrics.getNumberOfTimeoutCalls())
                .build());
        
        builder.withDetail("timestamp", LocalDateTime.now());
        
        // Check if circuit breaker is open or half-open
        if (circuitBreakerState == CircuitBreaker.State.OPEN || 
            circuitBreakerState == CircuitBreaker.State.HALF_OPEN) {
            builder.down().withDetail("warning", "Circuit breaker is not in closed state");
        }
        
        return builder.build();
    }
    
    private Mono<Health> handleError(Throwable throwable) {
        log.error("Resilience health check failed", throwable);
        
        return Mono.just(Health.down()
                .withDetail("error", throwable.getMessage())
                .withDetail("timestamp", LocalDateTime.now())
                .build());
    }
}
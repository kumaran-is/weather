package com.weather.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class ResilienceConfig {
    
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }
    
    @Bean
    public CircuitBreaker weatherServiceCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .maxWaitDurationInHalfOpenState(Duration.ofSeconds(5))
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(
                        org.springframework.dao.DataAccessException.class,
                        java.sql.SQLException.class,
                        java.io.IOException.class
                )
                .build();
        
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("weather-service", config);
        
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> 
                        log.info("Circuit breaker state transition: {} -> {}", 
                                event.getStateTransition().getFromState(), 
                                event.getStateTransition().getToState()));
        
        circuitBreaker.getEventPublisher()
                .onCallNotPermitted(event -> 
                        log.warn("Circuit breaker call not permitted for: {}", event.getCircuitBreakerName()));
        
        return circuitBreaker;
    }
    
    @Bean
    public RetryRegistry retryRegistry() {
        return RetryRegistry.ofDefaults();
    }
    
    @Bean
    public Retry weatherServiceRetry(RetryRegistry retryRegistry) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .retryExceptions(
                        org.springframework.dao.DataAccessException.class,
                        java.sql.SQLException.class
                )
                .build();
        
        Retry retry = retryRegistry.retry("weather-service", config);
        
        retry.getEventPublisher()
                .onRetry(event -> 
                        log.warn("Retry attempt {} for: {}", 
                                event.getNumberOfRetryAttempts(), 
                                event.getName()));
        
        return retry;
    }
    
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        return RateLimiterRegistry.ofDefaults();
    }
    
    @Bean
    public RateLimiter weatherServiceRateLimiter(RateLimiterRegistry rateLimiterRegistry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(100)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofSeconds(1))
                .build();
        
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("weather-service", config);
        
        rateLimiter.getEventPublisher()
                .onAcquirePermission(event -> 
                        log.debug("Rate limiter permission acquired for: {}", event.getRateLimiterName()));
        
        rateLimiter.getEventPublisher()
                .onDrillDown(event -> 
                        log.warn("Rate limiter permission rejected for: {}", event.getRateLimiterName()));
        
        return rateLimiter;
    }
    
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        return TimeLimiterRegistry.ofDefaults();
    }
    
    @Bean
    public TimeLimiter weatherServiceTimeLimiter(TimeLimiterRegistry timeLimiterRegistry) {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(3))
                .cancelRunningFuture(true)
                .build();
        
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter("weather-service", config);
        
        timeLimiter.getEventPublisher()
                .onTimeout(event -> 
                        log.warn("Time limiter timeout for: {}", event.getTimeLimiterName()));
        
        return timeLimiter;
    }
}
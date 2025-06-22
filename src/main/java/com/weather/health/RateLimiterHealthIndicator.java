package com.weather.health;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

@Component("rateLimiterHealthIndicator")
@ConditionalOnProperty(
    name = "health.ratelimiter.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class RateLimiterHealthIndicator implements ReactiveHealthIndicator {

    private final RateLimiterRegistry rateLimiterRegistry;

    @Autowired
    public RateLimiterHealthIndicator(RateLimiterRegistry rateLimiterRegistry) {
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    @Override
    public Mono<Health> health() {
        Map<String, Health> rateLimiterDetails = rateLimiterRegistry.getAllRateLimiters()
            .stream()
            .collect(Collectors.toMap(
                RateLimiter::getName,
                rl -> {
                    RateLimiter.Metrics metrics = rl.getMetrics();
                    io.github.resilience4j.ratelimiter.RateLimiterConfig config = rl.getRateLimiterConfig();
                    Health.Builder builder = Health.up(); // Rate limiters are generally 'UP' unless misconfigured

                    // A high number of waiting threads might indicate a problem or bottleneck
                    if (metrics.getNumberOfWaitingThreads() > (config.getLimitForPeriod() * 0.8)) { // Example threshold: 80% of limit
                        builder.status(Status.UNKNOWN) // Or a custom "DEGRADED" status
                               .withDetail("warning", "High number of waiting threads");
                    }

                    return builder
                        .withDetail("availablePermissions", metrics.getAvailablePermissions())
                        .withDetail("numberOfWaitingThreads", metrics.getNumberOfWaitingThreads())
                        .withDetail("limitForPeriod", config.getLimitForPeriod())
                        .withDetail("limitRefreshPeriod", config.getLimitRefreshPeriod().toString())
                        .withDetail("timeoutDuration", config.getTimeoutDuration().toString())
                        .build();
                }
            ));

        // Aggregate status: if any rate limiter is UNKNOWN (e.g., too many waiting threads), overall is UNKNOWN.
        boolean anyUnknown = rateLimiterDetails.values().stream()
            .anyMatch(h -> h.getStatus().equals(Status.UNKNOWN));

        Health.Builder overallHealthBuilder;
        if (anyUnknown) {
            overallHealthBuilder = Health.status(Status.UNKNOWN).withDetail("summary", "One or more rate limiters showing concerning patterns");
        } else {
            overallHealthBuilder = Health.up().withDetail("summary", "All rate limiters are operational");
        }

        return Mono.just(overallHealthBuilder
            .withDetail("totalRateLimiters", rateLimiterDetails.size())
            .withDetails(rateLimiterDetails)
            .build());
    }

    @Override
    public String getName() {
        return "rateLimiters";
    }
}
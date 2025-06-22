package com.weather.health;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

@Component("timeLimiterHealthIndicator")
@ConditionalOnProperty(
    name = "health.timelimiter.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class TimeLimiterHealthIndicator implements ReactiveHealthIndicator {

    private final TimeLimiterRegistry timeLimiterRegistry;

    @Autowired
    public TimeLimiterHealthIndicator(TimeLimiterRegistry timeLimiterRegistry) {
        this.timeLimiterRegistry = timeLimiterRegistry;
    }

    @Override
    public Mono<Health> health() {
        Map<String, Health> timeLimiterDetails = timeLimiterRegistry.getAllTimeLimiters()
            .stream()
            .collect(Collectors.toMap(
                TimeLimiter::getName,
                tl -> {
                    // TimeLimiters don't have detailed metrics like CircuitBreakers or Retries.
                    // Health is primarily based on their configuration.
                    // A very short timeout might be a configuration concern.
                    Health.Builder builder = Health.up();
                    if (tl.getTimeLimiterConfig().getTimeoutDuration().toMillis() < 100) { // Example: warn if timeout is less than 100ms
                        builder.status(Status.UNKNOWN) // Or a custom "DEGRADED" status
                               .withDetail("warning", "Configured timeout is very short: " + tl.getTimeLimiterConfig().getTimeoutDuration().toString());
                    }
                    return builder
                        .withDetail("timeoutDuration", tl.getTimeLimiterConfig().getTimeoutDuration().toString())
                        .withDetail("cancelRunningFuture", tl.getTimeLimiterConfig().shouldCancelRunningFuture())
                        .build();
                }
            ));
        
        boolean anyUnknown = timeLimiterDetails.values().stream()
            .anyMatch(h -> h.getStatus().equals(Status.UNKNOWN));

        Health.Builder overallHealthBuilder;
        if (anyUnknown) {
            overallHealthBuilder = Health.status(Status.UNKNOWN).withDetail("summary", "One or more time limiters have concerning configurations");
        } else {
            overallHealthBuilder = Health.up().withDetail("summary", "All time limiters are properly configured");
        }

        return Mono.just(overallHealthBuilder
            .withDetail("totalTimeLimiters", timeLimiterDetails.size())
            .withDetails(timeLimiterDetails)
            .build());
    }

    @Override
    public String getName() {
        return "timeLimiters";
    }
}
package com.weather.health;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

@Component("retryHealthIndicator")
@ConditionalOnProperty(
    name = "health.retry.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class RetryHealthIndicator implements ReactiveHealthIndicator {

    private final RetryRegistry retryRegistry;

    @Autowired
    public RetryHealthIndicator(RetryRegistry retryRegistry) {
        this.retryRegistry = retryRegistry;
    }

    @Override
    public Mono<Health> health() {
        Map<String, Health> retryDetails = retryRegistry.getAllRetries()
            .stream()
            .collect(Collectors.toMap(
                Retry::getName,
                retry -> {
                    Retry.Metrics metrics = retry.getMetrics();
                    Health.Builder builder = Health.up(); // Retries themselves don't have an UP/DOWN state, but metrics can indicate issues.

                    // Example: If there are failed calls without success, it might be a concern.
                    // This logic can be more sophisticated based on requirements.
                    if (metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt() == 0 && metrics.getNumberOfFailedCallsWithoutRetryAttempt() > 0) {
                         builder.status(Status.UNKNOWN).withDetail("warning", "Ongoing failures without successful retries observed.");
                    }
                     if (metrics.getNumberOfFailedCallsWithRetryAttempt() > metrics.getNumberOfSuccessfulCallsWithRetryAttempt() * 2 && metrics.getNumberOfFailedCallsWithRetryAttempt() > 10) { // Arbitrary threshold
                        builder.status(Status.UNKNOWN).withDetail("warning", "High number of failed calls even after retries.");
                    }

                    return builder
                        .withDetail("successfulCallsWithoutRetry", metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt())
                        .withDetail("successfulCallsWithRetry", metrics.getNumberOfSuccessfulCallsWithRetryAttempt())
                        .withDetail("failedCallsWithoutRetry", metrics.getNumberOfFailedCallsWithoutRetryAttempt())
                        .withDetail("failedCallsWithRetry", metrics.getNumberOfFailedCallsWithRetryAttempt())
                        .build();
                }
            ));

        boolean anyUnknown = retryDetails.values().stream()
            .anyMatch(h -> h.getStatus().equals(Status.UNKNOWN));

        Health.Builder overallHealthBuilder;
        if (anyUnknown) {
            overallHealthBuilder = Health.status(Status.UNKNOWN).withDetail("summary", "One or more retry mechanisms showing concerning patterns");
        } else {
            overallHealthBuilder = Health.up().withDetail("summary", "All retry mechanisms are operational");
        }

        return Mono.just(overallHealthBuilder
            .withDetail("totalRetries", retryDetails.size())
            .withDetails(retryDetails)
            .build());
    }

    @Override
    public String getName() {
        return "retries";
    }
}
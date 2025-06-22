package com.weather.health;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

@Component("bulkheadHealthIndicator")
@ConditionalOnProperty(
    name = "health.bulkhead.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class BulkheadHealthIndicator implements ReactiveHealthIndicator {

    private final BulkheadRegistry bulkheadRegistry;

    @Autowired
    public BulkheadHealthIndicator(BulkheadRegistry bulkheadRegistry) {
        this.bulkheadRegistry = bulkheadRegistry;
    }

    @Override
    public Mono<Health> health() {
        Map<String, Health> bulkheadDetails = bulkheadRegistry.getAllBulkheads()
            .stream()
            .collect(Collectors.toMap(
                Bulkhead::getName,
                bulkhead -> {
                    Bulkhead.Metrics metrics = bulkhead.getMetrics();
                    Health.Builder builder = Health.up(); // Bulkheads are generally 'UP' unless misconfigured

                    // Check if bulkhead is at capacity or has concerning usage patterns
                    int availableCalls = metrics.getAvailableConcurrentCalls();
                    int maxCalls = metrics.getMaxAllowedConcurrentCalls();
                    double usagePercentage = ((double) (maxCalls - availableCalls) / maxCalls) * 100;

                    // If bulkhead is over 90% capacity, mark as UNKNOWN/concerning
                    if (usagePercentage > 90.0) {
                        builder.status(Status.UNKNOWN)
                               .withDetail("warning", "Bulkhead is at high capacity: " + String.format("%.1f%%", usagePercentage));
                    }
                    
                    // If no calls are available, it's critically overloaded
                    if (availableCalls == 0) {
                        builder.status(Status.UNKNOWN)
                               .withDetail("warning", "Bulkhead is at full capacity - rejecting calls");
                    }

                    return builder
                        .withDetail("availableConcurrentCalls", availableCalls)
                        .withDetail("maxAllowedConcurrentCalls", maxCalls)
                        .withDetail("usagePercentage", String.format("%.1f%%", usagePercentage))
                        .build();
                }
            ));

        // Aggregate status: if any bulkhead is UNKNOWN (e.g., at high capacity), overall is UNKNOWN.
        boolean anyUnknown = bulkheadDetails.values().stream()
            .anyMatch(h -> h.getStatus().equals(Status.UNKNOWN));

        Health.Builder overallHealthBuilder;
        if (anyUnknown) {
            overallHealthBuilder = Health.status(Status.UNKNOWN).withDetail("summary", "One or more bulkheads are at concerning capacity levels");
        } else {
            overallHealthBuilder = Health.up().withDetail("summary", "All bulkheads are operating within normal capacity");
        }

        return Mono.just(overallHealthBuilder
            .withDetail("totalBulkheads", bulkheadDetails.size())
            .withDetails(bulkheadDetails)
            .build());
    }

    @Override
    public String getName() {
        return "bulkheads";
    }
}
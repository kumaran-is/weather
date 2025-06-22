package com.weather.health;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Endpoint(id = "deephealth") // Exposes endpoint at /management/deephealth
public class DeepHealthEndpoint {

    private final HealthIndicatorAggregator healthIndicatorAggregator;

    public DeepHealthEndpoint(HealthIndicatorAggregator healthIndicatorAggregator) {
        this.healthIndicatorAggregator = healthIndicatorAggregator;
    }

    @ReadOperation
    public Mono<Health> health() {
        return healthIndicatorAggregator.aggregateHealth();
    }

    // Optional: Add a @WriteOperation or @DeleteOperation if needed for this endpoint,
    // for example, to trigger a refresh of health statuses or clear caches.
    // Ensure proper security if write/delete operations are added.
}
package com.weather.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

@Component("circuitBreakerHealthIndicator")
@ConditionalOnProperty(
    name = "health.circuitbreaker.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class CircuitBreakerHealthIndicator implements ReactiveHealthIndicator {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    public CircuitBreakerHealthIndicator(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Override
    public Mono<Health> health() {
        Map<String, Health> circuitBreakerHealthDetails = circuitBreakerRegistry.getAllCircuitBreakers()
            .stream()
            .collect(Collectors.toMap(
                CircuitBreaker::getName,
                cb -> {
                    CircuitBreaker.State state = cb.getState();
                    Health.Builder builder;
                    switch (state) {
                        case CLOSED:
                            builder = Health.up();
                            break;
                        case OPEN:
                            builder = Health.down();
                            break;
                        case HALF_OPEN:
                            builder = Health.status(Status.UNKNOWN).withDetail("reason", "Circuit breaker is half-open");
                            break;
                        case FORCED_OPEN:
                            builder = Health.status(new Status("FORCED_OPEN", "Circuit breaker is forced open")).down();
                            break;
                        case DISABLED:
                            builder = Health.status(Status.UNKNOWN).withDetail("reason", "Circuit breaker is disabled");
                            break;
                        default:
                            builder = Health.unknown();
                            break;
                    }
                    return builder
                        .withDetail("state", state.name())
                        .withDetail("failureRate", String.format("%.2f%%", cb.getMetrics().getFailureRate()))
                        .withDetail("slowCallRate", String.format("%.2f%%", cb.getMetrics().getSlowCallRate()))
                        .withDetail("bufferedCalls", cb.getMetrics().getNumberOfBufferedCalls())
                        .withDetail("failedCalls", cb.getMetrics().getNumberOfFailedCalls())
                        .withDetail("slowCalls", cb.getMetrics().getNumberOfSlowCalls())
                        .withDetail("notPermittedCalls", cb.getMetrics().getNumberOfNotPermittedCalls())
                        .withDetail("successfulCalls", cb.getMetrics().getNumberOfSuccessfulCalls())
                        .build();
                }
            ));

        // Aggregate status: if any circuit breaker is DOWN or FORCED_OPEN, overall is DOWN.
        // If any is HALF_OPEN or UNKNOWN, overall is UNKNOWN. Otherwise UP.
        boolean anyDown = circuitBreakerHealthDetails.values().stream()
            .anyMatch(h -> h.getStatus().equals(Status.DOWN) || "FORCED_OPEN".equals(h.getStatus().getCode()));
        boolean anyUnknownOrHalfOpen = circuitBreakerHealthDetails.values().stream()
            .anyMatch(h -> h.getStatus().equals(Status.UNKNOWN) || h.getStatus().getCode().contains("HALF_OPEN"));

        Health.Builder overallHealthBuilder;
        if (anyDown) {
            overallHealthBuilder = Health.down().withDetail("summary", "One or more circuit breakers are open or forced open");
        } else if (anyUnknownOrHalfOpen) {
            overallHealthBuilder = Health.status(Status.UNKNOWN).withDetail("summary", "One or more circuit breakers are in half-open or unknown state");
        } else {
            overallHealthBuilder = Health.up().withDetail("summary", "All circuit breakers are operational");
        }

        return Mono.just(overallHealthBuilder
            .withDetail("totalCircuitBreakers", circuitBreakerHealthDetails.size())
            .withDetails(circuitBreakerHealthDetails)
            .build());
    }

    @Override
    public String getName() {
        return "circuitBreakers";
    }
}
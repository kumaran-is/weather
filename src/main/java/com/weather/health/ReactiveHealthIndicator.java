package com.weather.health;

import org.springframework.boot.actuate.health.Health;
import reactor.core.publisher.Mono;

/**
 * Custom ReactiveHealthIndicator interface.
 * While Spring Boot Actuator provides {@link org.springframework.boot.actuate.health.ReactiveHealthIndicator},
 * this custom interface is created to provide additional functionality for weather service health indicators.
 * Implementations should provide a reactive way to determine component health.
 */
@FunctionalInterface
public interface ReactiveHealthIndicator extends org.springframework.boot.actuate.health.ReactiveHealthIndicator {

    /**
     * Return an indication of health.
     *
     * @return a {@link Mono} that provides the {@link Health}
     */
    @Override
    Mono<Health> health();

    /**
     * Default method to get the name of the health indicator, can be overridden.
     * Useful for the aggregator.
     * 
     * @return the name of this health indicator
     */
    default String getName() {
        String simpleName = getClass().getSimpleName();
        if (simpleName.endsWith("HealthIndicator")) {
            return simpleName.substring(0, simpleName.length() - "HealthIndicator".length());
        }
        return simpleName;
    }
}
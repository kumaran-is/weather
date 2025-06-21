package com.weather.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component("applicationHealthIndicator")
@ConditionalOnProperty(
    name = "health.application.enabled",
    havingValue = "true",
    matchIfMissing = true // Enabled by default
)
public class ApplicationHealthIndicator implements ReactiveHealthIndicator {

    private static final String INDICATOR_NAME = "application";

    @Override
    public Mono<Health> health() {
        // This is a basic health check. More sophisticated checks could be added,
        // e.g., checking critical internal states or configurations.
        return Mono.fromCallable(() -> Health.status(Status.UP)
                .withDetail("name", INDICATOR_NAME)
                .withDetail("status", "Weather Service Application is running")
                .withDetail("timestamp", Instant.now().toString())
                .withDetail("service", "weather-service")
                .withDetail("version", "1.0.0")
                .build()
        ).onErrorResume(throwable -> {
            Exception exToReport;
            if (throwable instanceof Exception) {
                exToReport = (Exception) throwable;
            } else {
                exToReport = new RuntimeException("Health check failed due to Throwable: " + throwable.getMessage(), throwable);
            }
            return Mono.just(
                Health.down(exToReport)
                    .withDetail("name", INDICATOR_NAME)
                    .withDetail("error", throwable.getClass().getName() + ": " + throwable.getMessage())
                    .build());
        });
    }

    @Override
    public String getName() {
        return "application";
    }
}
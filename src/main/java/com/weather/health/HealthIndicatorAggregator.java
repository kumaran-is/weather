package com.weather.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.AbstractMap;
import java.util.Map;

@Slf4j
@Component
public class HealthIndicatorAggregator {

    private final ApplicationContext applicationContext;

    public HealthIndicatorAggregator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public Mono<Health> aggregateHealth() {
        // Get all beans implementing our custom ReactiveHealthIndicator interface
        Map<String, ReactiveHealthIndicator> healthIndicators =
            applicationContext.getBeansOfType(ReactiveHealthIndicator.class);

        if (healthIndicators.isEmpty()) {
            log.warn("No ReactiveHealthIndicator beans found in the application context.");
            return Mono.just(Health.up().withDetail("message", "No custom health indicators configured.").build());
        }
        
        log.debug("Found {} ReactiveHealthIndicator beans to aggregate.", healthIndicators.size());

        return Flux.fromIterable(healthIndicators.entrySet())
            .flatMap(entry -> {
                final String name = cleanUpName(entry.getKey());
                log.debug("Querying health for indicator: {}", name);
                return entry.getValue().health()
                    .map(health -> {
                        log.debug("Health for {}: {}", name, health.getStatus());
                        return new AbstractMap.SimpleEntry<>(name, health);
                    })
                    .onErrorResume(throwable -> {
                        log.error("Error querying health for indicator: {}", name, throwable);
                        Exception exToReport;
                        if (throwable instanceof Exception) {
                            exToReport = (Exception) throwable;
                        } else {
                            exToReport = new RuntimeException("Health check failed for " + name + " due to Throwable: " + throwable.getMessage(), throwable);
                        }
                        Health errorHealth = Health.down(exToReport)
                            .withDetail("error", throwable.getClass().getSimpleName() + ": " + throwable.getMessage())
                            .build();
                        return Mono.just(new AbstractMap.SimpleEntry<>(name, errorHealth));
                    });
            })
            .collectMap(Map.Entry::getKey, Map.Entry::getValue)
            .map(healthMap -> {
                // Determine overall status based on individual health statuses
                boolean hasDown = healthMap.values().stream()
                    .anyMatch(h -> Status.DOWN.equals(h.getStatus()));
                boolean hasDegraded = healthMap.values().stream()
                    .anyMatch(h -> "DEGRADED".equals(h.getStatus().getCode())); // Using code for custom status
                boolean hasUnknown = healthMap.values().stream()
                    .anyMatch(h -> Status.UNKNOWN.equals(h.getStatus()));

                Status overallStatus;
                String reason;

                if (hasDown) {
                    overallStatus = Status.DOWN;
                    reason = "One or more critical components are DOWN.";
                } else if (hasDegraded) {
                    overallStatus = new Status("DEGRADED", "One or more components are DEGRADED.");
                    reason = "One or more components are DEGRADED.";
                } else if (hasUnknown) {
                    overallStatus = Status.UNKNOWN;
                    reason = "Status of one or more components is UNKNOWN.";
                } else {
                    overallStatus = Status.UP;
                    reason = "All systems operational.";
                }
                
                log.info("Aggregated health status: {}, Reason: {}", overallStatus.getCode(), reason);

                return Health.status(overallStatus)
                    .withDetail("statusSummary", reason)
                    .withDetail("totalIndicators", healthMap.size())
                    .withDetail("service", "weather-service")
                    .withDetail("version", "1.0.0")
                    .withDetails(healthMap) // Embeds individual health details
                    .build();
            });
    }

    private String cleanUpName(String beanName) {
        // Remove common suffixes like "HealthIndicator" or "Indicator" for cleaner display names
        if (beanName.endsWith("HealthIndicator")) {
            return beanName.substring(0, beanName.length() - "HealthIndicator".length());
        }
        if (beanName.endsWith("Indicator")) {
            return beanName.substring(0, beanName.length() - "Indicator".length());
        }
        // Remove "custom" prefix if present from bean names like "customCircuitBreakerHealthIndicator"
        if (beanName.startsWith("custom") && beanName.length() > "custom".length() && Character.isUpperCase(beanName.charAt("custom".length()))) {
            return Character.toLowerCase(beanName.charAt("custom".length())) + beanName.substring("custom".length() + 1);
        }
        return beanName;
    }
}
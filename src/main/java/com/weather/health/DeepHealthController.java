package com.weather.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.ReactiveHealthIndicator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/management")
@RequiredArgsConstructor
public class DeepHealthController {
    
    private final List<ReactiveHealthIndicator> healthIndicators;
    
    @GetMapping("/deephealth")
    public Mono<Map<String, Object>> deepHealth() {
        log.debug("Executing deep health check");
        
        return Mono.fromCallable(() -> healthIndicators.stream()
                .collect(Collectors.toMap(
                        indicator -> indicator.getClass().getSimpleName(),
                        indicator -> indicator.health().block() // Note: blocking for simplicity in this endpoint
                )))
                .map(healthMap -> {
                    healthMap.put("timestamp", LocalDateTime.now());
                    healthMap.put("overallStatus", determineOverallHealth(healthMap));
                    return healthMap;
                })
                .doOnSuccess(result -> log.info("Deep health check completed"))
                .onErrorResume(throwable -> {
                    log.error("Deep health check failed", throwable);
                    return Mono.just(Map.of(
                            "status", "DOWN",
                            "error", throwable.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
                });
    }
    
    private String determineOverallHealth(Map<String, Object> healthMap) {
        boolean allUp = healthMap.values().stream()
                .filter(Health.class::isInstance)
                .map(Health.class::cast)
                .allMatch(health -> health.getStatus().getCode().equals("UP"));
        
        return allUp ? "UP" : "DOWN";
    }
}
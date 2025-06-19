package com.weather.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/management")
@RequiredArgsConstructor
public class DeepHealthController {
    
    private final List<ReactiveHealthIndicator> healthIndicators;
    
    @GetMapping("/deephealth")
    public Mono<Map<String, Object>> deepHealth() {
        log.debug("Executing deep health check");
        
        return Flux.fromIterable(healthIndicators)
                .flatMap(indicator -> 
                    indicator.health()
                        .map(health -> Map.entry(indicator.getClass().getSimpleName(), health))
                )
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .map(healthMap -> {
                    return Map.<String, Object>of(
                        "timestamp", LocalDateTime.now(),
                        "overallStatus", determineOverallHealth(healthMap),
                        "components", healthMap
                    );
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
    
    private String determineOverallHealth(Map<String, Health> healthMap) {
        boolean allUp = healthMap.values().stream()
                .allMatch(health -> health.getStatus().getCode().equals("UP"));
        
        return allUp ? "UP" : "DOWN";
    }
}
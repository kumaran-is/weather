package com.weather.health;

import com.weather.repository.WeatherDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseHealthIndicator implements ReactiveHealthIndicator {
    
    private final WeatherDataRepository weatherDataRepository;
    
    @Override
    public Mono<Health> health() {
        return checkDatabaseHealth()
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(this::handleError);
    }
    
    private Mono<Health> checkDatabaseHealth() {
        LocalDateTime startTime = LocalDateTime.now();
        
        return weatherDataRepository.count()
                .map(count -> {
                    LocalDateTime endTime = LocalDateTime.now();
                    Duration responseTime = Duration.between(startTime, endTime);
                    
                    Health.Builder builder = Health.up()
                            .withDetail("database", "R2DBC")
                            .withDetail("totalRecords", count)
                            .withDetail("responseTime", responseTime.toMillis() + "ms")
                            .withDetail("timestamp", LocalDateTime.now());
                    
                    if (responseTime.toMillis() > 1000) {
                        builder.withDetail("warning", "Database response time is slow");
                    }
                    
                    return builder.build();
                });
    }
    
    private Mono<Health> handleError(Throwable throwable) {
        log.error("Database health check failed", throwable);
        
        return Mono.just(Health.down()
                .withDetail("database", "R2DBC")
                .withDetail("error", throwable.getMessage())
                .withDetail("timestamp", LocalDateTime.now())
                .build());
    }
}
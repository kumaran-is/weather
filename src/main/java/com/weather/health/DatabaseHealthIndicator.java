package com.weather.health;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component("databaseHealthIndicator")
@ConditionalOnProperty(
    name = "health.db.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class DatabaseHealthIndicator extends AbstractReactiveHealthIndicator implements com.weather.health.ReactiveHealthIndicator {

    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final String defaultValidationQuery = "SELECT 1"; // Common validation query

    @Autowired
    public DatabaseHealthIndicator(ConnectionFactory connectionFactory) {
        if (connectionFactory == null) {
            throw new IllegalArgumentException("ConnectionFactory must not be null");
        }
        // Using R2dbcEntityTemplate for a more common way to interact with the DB
        this.r2dbcEntityTemplate = new R2dbcEntityTemplate(connectionFactory);
    }

    @Override
    protected Mono<Health> doHealthCheck(Health.Builder builder) {
        return r2dbcEntityTemplate.getDatabaseClient()
            .sql(defaultValidationQuery)
            .fetch()
            .first() // We only care that the query executes, not the result
            .hasElement()
            .map(hasElement -> {
                if (hasElement) {
                    return Health.up()
                        .withDetail("database", "H2")
                        .withDetail("validationQuery", defaultValidationQuery)
                        .withDetail("status", "Connection successful")
                        .build();
                } else {
                    return Health.down()
                        .withDetail("validationQuery", "Returned no element")
                        .withDetail("database", "H2")
                        .build();
                }
            })
            .onErrorResume(ex ->
                Mono.just(
                    Health.down(ex)
                        .withDetail("error", ex.getClass().getName() + ": " + ex.getMessage())
                        .withDetail("validationQuery", defaultValidationQuery)
                        .withDetail("database", "H2")
                        .build()
                )
            );
    }

    // The health() method is inherited from AbstractReactiveHealthIndicator
    // and calls doHealthCheck(Health.Builder)

    @Override
    public String getName() {
        return "database";
    }
}
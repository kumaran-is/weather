# Health Indicators Implementation Guide

## 📑 Table of Contents
- [Overview](#overview)
- [Architecture & Design Approach](#architecture--design-approach)
- [Implementation Details](#implementation-details)
  - [Custom ReactiveHealthIndicator Interface](#1-custom-reactivehealthindicator-interface)
  - [Individual Health Indicators](#2-individual-health-indicators)
  - [Health Indicator Aggregator](#3-health-indicator-aggregator)
  - [Deep Health Endpoint](#4-deep-health-endpoint)
  - [Configuration Properties](#5-configuration-properties)
- [Health Check Endpoints](#health-check-endpoints)
- [Health Status Logic](#health-status-logic)
- [Monitoring and Observability](#monitoring-and-observability)
- [Configuration Best Practices](#configuration-best-practices)
- [Troubleshooting Guide](#troubleshooting-guide)
- [Integration with External Monitoring](#integration-with-external-monitoring)

## Overview

This document describes the comprehensive health monitoring system implemented in the Weather Service application. We have built a modular, aggregated health checking system that provides detailed visibility into application health, database connectivity, and all resilience patterns.

## Architecture & Design Approach

### Design Principles

1. **Separation of Concerns**: Each health indicator focuses on a specific component or pattern
2. **Aggregated Health View**: Individual indicators are aggregated to provide overall system health
3. **Conditional Enablement**: Each indicator can be enabled/disabled via configuration
4. **Actuator Integration**: Leverages Spring Boot Actuator for standardized health endpoints
5. **Reactive Implementation**: All health checks are non-blocking and reactive

### Health Monitoring Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   Health Endpoints                         │
│  /management/health      /management/deephealth            │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│               HealthIndicatorAggregator                    │
│         Aggregates all health indicators                   │
│         Determines overall system status                   │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                Individual Health Indicators                │
│  ┌─────────────────┐ ┌─────────────────┐ ┌───────────────┐ │
│  │  Application    │ │    Database     │ │ Resilience    │ │
│  │     Health      │ │     Health      │ │   Patterns    │ │
│  └─────────────────┘ └─────────────────┘ └───────────────┘ │
│                                                             │
│  ┌─────────────────┐ ┌─────────────────┐ ┌───────────────┐ │
│  │ CircuitBreaker  │ │     Retry       │ │ RateLimiter   │ │
│  │     Health      │ │     Health      │ │    Health     │ │
│  └─────────────────┘ └─────────────────┘ └───────────────┘ │
│                                                             │
│  ┌─────────────────┐ ┌─────────────────┐                   │
│  │  TimeLimiter    │ │   Bulkhead      │                   │
│  │     Health      │ │     Health      │                   │
│  └─────────────────┘ └─────────────────┘                   │
└─────────────────────────────────────────────────────────────┘
```

## Implementation Details

### 1. Custom ReactiveHealthIndicator Interface

We created a custom interface that extends Spring Boot's ReactiveHealthIndicator to add naming functionality:

```java
@FunctionalInterface
public interface ReactiveHealthIndicator extends org.springframework.boot.actuate.health.ReactiveHealthIndicator {

    @Override
    Mono<Health> health();

    default String getName() {
        String simpleName = getClass().getSimpleName();
        if (simpleName.endsWith("HealthIndicator")) {
            return simpleName.substring(0, simpleName.length() - "HealthIndicator".length());
        }
        return simpleName;
    }
}
```

**Benefits:**
- **Consistent Naming**: Automatic name extraction for cleaner health reports
- **Spring Integration**: Fully compatible with Spring Boot Actuator
- **Reactive Support**: Non-blocking health checks

### 2. Individual Health Indicators

#### Application Health Indicator
```java
@Component("applicationHealthIndicator")
@ConditionalOnProperty(
    name = "health.application.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class ApplicationHealthIndicator implements ReactiveHealthIndicator {
    
    @Override
    public Mono<Health> health() {
        return Mono.fromCallable(() -> Health.status(Status.UP)
                .withDetail("service", "weather-service")
                .withDetail("version", "1.0.0")
                .withDetail("status", "Weather Service Application is running")
                .build());
    }
}
```

#### Database Health Indicator
```java
@Component("databaseHealthIndicator")
public class DatabaseHealthIndicator extends AbstractReactiveHealthIndicator 
    implements ReactiveHealthIndicator {
    
    @Override
    protected Mono<Health> doHealthCheck(Health.Builder builder) {
        return r2dbcEntityTemplate.getDatabaseClient()
            .sql("SELECT 1")
            .fetch()
            .first()
            .hasElement()
            .map(hasElement -> hasElement ? 
                Health.up().withDetail("database", "H2").build() :
                Health.down().withDetail("error", "No connection").build());
    }
}
```

#### Resilience Pattern Health Indicators

Each resilience pattern has its own dedicated health indicator:

##### Circuit Breaker Health Indicator
```java
@Component("circuitBreakerHealthIndicator")
public class CircuitBreakerHealthIndicator implements ReactiveHealthIndicator {
    
    @Override
    public Mono<Health> health() {
        Map<String, Health> circuitBreakerHealthDetails = circuitBreakerRegistry.getAllCircuitBreakers()
            .stream()
            .collect(Collectors.toMap(
                CircuitBreaker::getName,
                cb -> {
                    CircuitBreaker.State state = cb.getState();
                    Health.Builder builder = switch (state) {
                        case CLOSED -> Health.up();
                        case OPEN -> Health.down();
                        case HALF_OPEN -> Health.status(Status.UNKNOWN);
                        case FORCED_OPEN -> Health.down();
                        case DISABLED -> Health.status(Status.UNKNOWN);
                        default -> Health.unknown();
                    };
                    
                    return builder
                        .withDetail("state", state.name())
                        .withDetail("failureRate", String.format("%.2f%%", cb.getMetrics().getFailureRate()))
                        .withDetail("bufferedCalls", cb.getMetrics().getNumberOfBufferedCalls())
                        .build();
                }
            ));
    }
}
```

**Key Features per Resilience Health Indicator:**

| Health Indicator | Monitors | Key Metrics | Warning Conditions |
|------------------|----------|-------------|-------------------|
| **CircuitBreaker** | All circuit breaker instances | State, failure rate, call counts | OPEN/HALF_OPEN states |
| **Retry** | All retry mechanisms | Success/failure counts by attempt | High failure rates after retries |
| **RateLimiter** | All rate limiters | Available permissions, waiting threads | High number of waiting threads |
| **TimeLimiter** | All time limiters | Timeout configurations | Very short timeout durations |
| **Bulkhead** | All bulkheads | Available/max concurrent calls | High capacity utilization (>90%) |

### 3. Health Indicator Aggregator

The aggregator collects all individual health indicators and determines overall system health:

```java
@Component
public class HealthIndicatorAggregator {

    public Mono<Health> aggregateHealth() {
        Map<String, ReactiveHealthIndicator> healthIndicators =
            applicationContext.getBeansOfType(ReactiveHealthIndicator.class);

        return Flux.fromIterable(healthIndicators.entrySet())
            .flatMap(entry -> {
                final String name = cleanUpName(entry.getKey());
                return entry.getValue().health()
                    .map(health -> new AbstractMap.SimpleEntry<>(name, health))
                    .onErrorResume(throwable -> {
                        Health errorHealth = Health.down()
                            .withDetail("error", throwable.getMessage())
                            .build();
                        return Mono.just(new AbstractMap.SimpleEntry<>(name, errorHealth));
                    });
            })
            .collectMap(Map.Entry::getKey, Map.Entry::getValue)
            .map(this::determineOverallHealth);
    }
}
```

**Aggregation Logic:**
1. **DOWN** - If any critical component is DOWN
2. **DEGRADED** - If any component is in DEGRADED state
3. **UNKNOWN** - If any component status is UNKNOWN
4. **UP** - If all components are operational

### 4. Deep Health Endpoint

Custom actuator endpoint for comprehensive health view:

```java
@Component
@Endpoint(id = "deephealth")
public class DeepHealthEndpoint {

    @ReadOperation
    public Mono<Health> health() {
        return healthIndicatorAggregator.aggregateHealth();
    }
}
```

**Benefits of Actuator Endpoint:**
- **Security Integration**: Inherits actuator security configuration
- **Consistent Format**: Follows Spring Boot health response format
- **Management Integration**: Available at `/management/deephealth`

### 5. Configuration Properties

All health indicators can be enabled/disabled through configuration:

```yaml
# Health indicator configuration
health:
  application:
    enabled: true
  db:
    enabled: true
  circuitbreaker:
    enabled: true
  retry:
    enabled: true
  ratelimiter:
    enabled: true
  timelimiter:
    enabled: true
  bulkhead:
    enabled: true

# Spring Boot Actuator configuration
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,deephealth
  endpoint:
    health:
      show-details: when_authorized
  health:
    circuitbreakers:
      enabled: true
    ratelimiters:
      enabled: true
    retries:
      enabled: true
    timelimiters:
      enabled: true
    bulkheads:
      enabled: true
```

## Health Check Endpoints

### 1. Standard Health Endpoint (`/management/health`)

Returns aggregated health with individual component details:

```json
{
  "status": "UP",
  "components": {
    "application": {
      "status": "UP",
      "details": {
        "service": "weather-service",
        "version": "1.0.0"
      }
    },
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "totalCircuitBreakers": 8,
        "createWeatherDataDb": {
          "status": "UP",
          "details": {
            "state": "CLOSED",
            "failureRate": "0.00%",
            "bufferedCalls": 0
          }
        }
      }
    },
    "database": {
      "status": "UP",
      "details": {
        "database": "H2",
        "validationQuery": "SELECT 1"
      }
    }
  }
}
```

### 2. Deep Health Endpoint (`/management/deephealth`)

Returns comprehensive aggregated health view:

```json
{
  "status": "UP",
  "details": {
    "statusSummary": "All systems operational.",
    "totalIndicators": 7,
    "service": "weather-service",
    "version": "1.0.0",
    "application": {
      "status": "UP"
    },
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "summary": "All circuit breakers are operational",
        "totalCircuitBreakers": 8
      }
    },
    "retries": {
      "status": "UP",
      "details": {
        "summary": "All retry mechanisms are operational"
      }
    }
  }
}
```

## Health Status Logic

### Individual Component Status

Each health indicator determines its status based on component-specific criteria:

#### Circuit Breaker Status Logic
```java
Health.Builder builder = switch (state) {
    case CLOSED -> Health.up();                    // Normal operation
    case OPEN -> Health.down();                    // Failing, calls blocked
    case HALF_OPEN -> Health.status(Status.UNKNOWN); // Testing recovery
    case FORCED_OPEN -> Health.down();             // Manually opened
    case DISABLED -> Health.status(Status.UNKNOWN); // Disabled
    default -> Health.unknown();
};
```

#### Rate Limiter Status Logic
```java
if (metrics.getNumberOfWaitingThreads() > (config.getLimitForPeriod() * 0.8)) {
    builder.status(Status.UNKNOWN)
           .withDetail("warning", "High number of waiting threads");
}
```

#### Bulkhead Status Logic
```java
double usagePercentage = ((double) (maxCalls - availableCalls) / maxCalls) * 100;
if (usagePercentage > 90.0) {
    builder.status(Status.UNKNOWN)
           .withDetail("warning", "Bulkhead is at high capacity");
}
```

### Aggregated Status Logic

The aggregator uses a priority-based approach to determine overall system health:

```java
Status overallStatus;
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
```

## Monitoring and Observability

### 1. Real-time Health Monitoring

Health indicators provide real-time visibility into:

- **Circuit Breaker States**: CLOSED, OPEN, HALF_OPEN transitions
- **Retry Patterns**: Success rates after retry attempts
- **Rate Limiting**: Available permissions and waiting threads
- **Timeout Issues**: Configuration validation and warnings
- **Capacity Utilization**: Bulkhead usage patterns
- **Database Connectivity**: Connection health and response times

### 2. Integration with Metrics

Health indicators complement the metrics system:

```yaml
weather:
  metrics:
    enabled: true
    included-api-paths:
      - "/api/v1/"
```

### 3. Logging Integration

All health check failures are logged with context:

```java
.onErrorResume(throwable -> {
    log.error("Error querying health for indicator: {}", name, throwable);
    Health errorHealth = Health.down()
        .withDetail("error", throwable.getMessage())
        .build();
    return Mono.just(errorHealth);
});
```

## Configuration Best Practices

### 1. Environment-Specific Health Configuration

```yaml
# Development Environment
health:
  circuitbreaker:
    enabled: true
  retry:
    enabled: true
  ratelimiter:
    enabled: false    # Disable in dev for easier testing

# Production Environment
health:
  circuitbreaker:
    enabled: true
  retry:
    enabled: true
  ratelimiter:
    enabled: true     # Enable all checks in production
```

### 2. Security Configuration

```yaml
management:
  endpoint:
    health:
      show-details: when_authorized  # Hide details from unauthorized users
  endpoints:
    web:
      exposure:
        include: health,deephealth
```

### 3. Health Check Timeouts

```yaml
management:
  endpoint:
    health:
      timeout: 10s    # Overall health check timeout
```

## Troubleshooting Guide

### Common Health Check Issues

1. **Health Indicator Not Appearing**
   - Check `@ConditionalOnProperty` configuration
   - Verify bean is being registered
   - Check component scan includes health package

2. **Health Check Timeouts**
   - Review individual indicator implementations
   - Check database connectivity for DatabaseHealthIndicator
   - Verify reactive chains don't block

3. **Incorrect Health Status**
   - Review status determination logic
   - Check threshold configurations
   - Verify metrics are being collected correctly

4. **Aggregated Health Issues**
   - Check HealthIndicatorAggregator bean registration
   - Verify all indicators implement ReactiveHealthIndicator
   - Review aggregation logic for edge cases

### Health Check Performance

Health checks are designed to be lightweight:

- **Reactive Implementation**: All checks are non-blocking
- **Cached Metrics**: Resilience metrics are cached and efficient
- **Quick Database Check**: Simple `SELECT 1` validation query
- **Timeout Protection**: Individual checks have timeout boundaries

## Integration with External Monitoring

### Prometheus Integration

Health status can be exported to Prometheus:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,prometheus
```

### Alerting Rules

Health indicators support alerting based on:

- Overall system health status changes
- Individual component health degradation
- Specific resilience pattern failures
- Database connectivity issues

This comprehensive health monitoring system provides complete visibility into the Weather Service's operational health, enabling proactive issue detection and resolution.
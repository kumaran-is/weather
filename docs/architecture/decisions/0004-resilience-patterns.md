# ADR-0004: Implement Resilience4j for Fault Tolerance

## Status
Accepted

## Context

Weather services must be resilient to various failure scenarios, including database outages, network issues, and external service dependencies. Without proper fault tolerance mechanisms, a single component failure can cascade and bring down the entire service.

### Identified Failure Scenarios

1. **Database Connection Issues**: R2DBC connection pool exhaustion or database unavailability
2. **Network Timeouts**: Slow database queries or external service calls
3. **Resource Exhaustion**: High load causing service degradation
4. **Downstream Dependencies**: Future integrations with weather APIs or other services
5. **Transient Failures**: Temporary network issues or database locks

### Evaluated Solutions

1. **Hystrix**: Netflix's circuit breaker (deprecated)
2. **Resilience4j**: Modern resilience library with reactive support
3. **Spring Retry**: Basic retry mechanisms
4. **Custom Implementation**: Build fault tolerance from scratch
5. **Service Mesh**: Istio/Envoy-based resilience (infrastructure level)

## Decision

We will implement **Resilience4j** with reactive support for comprehensive fault tolerance:

### Core Resilience Patterns

1. **Circuit Breaker**: Prevent cascade failures
2. **Retry**: Handle transient failures
3. **Time Limiter**: Prevent hanging operations
4. **Rate Limiter**: Control request volume
5. **Bulkhead**: Isolate critical resources

### Implementation Strategy

```java
@Service
public class WeatherDataServiceImpl implements WeatherDataService {
    
    @CircuitBreaker(name = "weather-service", fallbackMethod = "createWeatherDataFallback")
    @Retry(name = "weather-service")
    @TimeLimiter(name = "weather-service")
    @Transactional
    public Mono<WeatherDataResponse> createWeatherData(WeatherDataRequest request) {
        return validateRequest(request)
                .then(Mono.fromCallable(() -> mapper.toEntity(request)))
                .flatMap(repository::save)
                .map(mapper::toResponse);
    }
    
    // Fallback method for circuit breaker
    public Mono<WeatherDataResponse> createWeatherDataFallback(
            WeatherDataRequest request, Exception ex) {
        log.error("Circuit breaker activated for createWeatherData", ex);
        return Mono.error(new WeatherServiceException("Service temporarily unavailable"));
    }
}
```

## Consequences

### Positive

- **Improved Availability**: Service remains partially functional during failures
- **Cascade Prevention**: Circuit breakers prevent failure propagation
- **Auto-Recovery**: Automatic retry mechanisms for transient failures
- **Performance Protection**: Time limiters prevent hanging operations
- **Resource Protection**: Rate limiters prevent resource exhaustion
- **Reactive Integration**: Native support for Mono/Flux reactive types
- **Observability**: Built-in metrics and monitoring capabilities
- **Configuration Flexibility**: Environment-specific resilience settings

### Negative

- **Added Complexity**: More components to configure and monitor
- **Fallback Logic**: Need to implement meaningful fallback responses
- **Testing Complexity**: Resilience patterns require specific testing approaches
- **Configuration Overhead**: Multiple resilience patterns to tune
- **Latency Impact**: Additional processing overhead for resilience checks
- **Debug Complexity**: Failure scenarios may be harder to reproduce

### Neutral

- **Learning Curve**: Team needs to understand resilience patterns
- **Monitoring Requirements**: Additional metrics and alerting needed
- **Configuration Management**: More application properties to manage

## Compliance

### Configuration Standards

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-size: 10
        sliding-window-type: count_based
        minimum-number-of-calls: 5
        failure-rate-threshold: 50.0
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 3
        automatic-transition-from-open-to-half-open-enabled: true
        record-exceptions:
          - org.springframework.dao.DataAccessException
          - java.sql.SQLException
          - java.io.IOException
  
  retry:
    configs:
      default:
        max-attempts: 3
        wait-duration: 1s
        retry-exceptions:
          - org.springframework.dao.DataAccessException
          - java.sql.SQLException
  
  timelimiter:
    configs:
      default:
        timeout-duration: 3s
        cancel-running-future: true
  
  ratelimiter:
    configs:
      default:
        limit-for-period: 100
        limit-refresh-period: 1s
        timeout-duration: 1s
```

### Implementation Guidelines

1. **Circuit Breaker Usage**:
   - Apply to all external dependencies
   - Implement meaningful fallback methods
   - Configure appropriate failure thresholds

2. **Retry Configuration**:
   - Only retry on transient failures
   - Use exponential backoff for external calls
   - Limit retry attempts to prevent delays

3. **Time Limiter Settings**:
   - Set reasonable timeouts for operations
   - Consider database query complexity
   - Allow for network latency variations

4. **Rate Limiting**:
   - Protect against traffic spikes
   - Configure per-endpoint if needed
   - Consider burst capacity requirements

### Fallback Strategies

#### Data Retrieval Fallbacks
```java
// Return cached data or empty result
public Mono<WeatherDataResponse> getWeatherDataByIdFallback(Long id, Exception ex) {
    log.warn("Fallback triggered for weather data retrieval: {}", id, ex);
    return cacheService.getCachedWeatherData(id)
            .switchIfEmpty(Mono.error(new WeatherServiceException(
                "Weather data temporarily unavailable")));
}
```

#### Write Operation Fallbacks
```java
// Queue for later processing or return error
public Mono<WeatherDataResponse> createWeatherDataFallback(
        WeatherDataRequest request, Exception ex) {
    log.error("Circuit breaker activated for data creation", ex);
    // Option 1: Queue for later processing
    return queueService.queueForLaterProcessing(request)
            .then(Mono.error(new WeatherServiceException(
                "Request queued for processing")));
    
    // Option 2: Return service unavailable error
    return Mono.error(new WeatherServiceException(
        "Weather service temporarily unavailable"));
}
```

### Monitoring and Alerting

```java
// Custom metrics for circuit breaker states
@Component
public class ResilienceMetrics {
    
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void onCircuitBreakerStateTransition(CircuitBreakerOnStateTransitionEvent event) {
        meterRegistry.counter("circuit_breaker_state_transition",
                "name", event.getCircuitBreakerName(),
                "from", event.getStateTransition().getFromState().name(),
                "to", event.getStateTransition().getToState().name())
                .increment();
    }
}
```

### Testing Resilience

```java
@Test
void shouldHandleCircuitBreakerActivation() {
    // Simulate database failures to trigger circuit breaker
    when(repository.save(any())).thenReturn(Mono.error(new DataAccessException("DB Error")));
    
    // First few calls should retry and fail
    for (int i = 0; i < 5; i++) {
        StepVerifier.create(weatherService.createWeatherData(testRequest))
                .expectError(WeatherServiceException.class)
                .verify();
    }
    
    // Circuit breaker should now be open
    StepVerifier.create(weatherService.createWeatherData(testRequest))
            .expectError(WeatherServiceException.class)
            .verify();
}
```

### Environment-Specific Configuration

| Environment | Circuit Breaker Threshold | Retry Attempts | Timeout |
|-------------|---------------------------|----------------|---------|
| Development | 70% (lenient) | 2 | 5s |
| Testing | 60% (moderate) | 3 | 3s |
| Production | 50% (strict) | 3 | 2s |

## Notes

### Circuit Breaker States

1. **CLOSED**: Normal operation, calls pass through
2. **OPEN**: Failing fast, calls immediately fail
3. **HALF_OPEN**: Testing recovery, limited calls allowed

### Best Practices Implemented

1. **Fail Fast**: Circuit breakers prevent hanging operations
2. **Graceful Degradation**: Meaningful fallback responses
3. **Resource Isolation**: Bulkhead pattern for critical operations
4. **Observability**: Comprehensive metrics and logging
5. **Configuration Externalization**: Environment-specific settings

### Integration with Spring Boot Actuator

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,circuitbreakers,retries
  endpoint:
    health:
      show-details: when_authorized
```

### Performance Impact

- **Circuit Breaker**: ~0.1ms overhead per call
- **Retry**: Variable based on failure rate
- **Time Limiter**: Minimal overhead for timeout tracking
- **Rate Limiter**: ~0.05ms overhead per call

---

**Last Updated:** 2024-01-15  
**Authors:** Weather Service Team  
**Reviewers:** Platform Team, Architecture Review Board
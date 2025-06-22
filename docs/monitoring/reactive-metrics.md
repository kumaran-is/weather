# Reactive Metrics and Context Propagation

This document describes the automatic reactive metrics collection and context propagation system implemented in the Weather Service.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Configuration](#configuration)
- [Available Metrics](#available-metrics)
- [Metrics Endpoints](#metrics-endpoints)
- [Context Propagation](#context-propagation)
- [Troubleshooting](#troubleshooting)

## Overview

The Weather Service implements automatic metrics collection for reactive operations with the following features:

- **🎯 Configurable Endpoint Filtering**: Only collect metrics for specified API paths
- **🔄 Automatic Context Propagation**: Correlation IDs, timing, and user context flow through reactive chains
- **📊 Comprehensive Metrics**: Request timing, success/error rates, subscription tracking
- **🚀 Future-Proof**: Easy to add new API paths without code changes
- **⚡ Non-Intrusive**: Zero impact on business logic - handled at the filter level

## Architecture

### Components

1. **`ReactiveContextWebFilter`**: Main entry point that:
   - Automatically enriches reactive context for all requests
   - Collects timing metrics for configured API endpoints
   - Propagates correlation IDs and user context

2. **`ReactiveMetricsConfig`**: Provides utility classes for:
   - Timer management for reactive operations
   - Subscription tracking (active vs total)
   - Backpressure monitoring

3. **`MetricsProperties`**: Configuration properties for:
   - Enabling/disabling metrics collection
   - Defining which API paths to monitor
   - Controlling metric detail levels

4. **`MetricsFilterConfig`**: Filters built-in Spring metrics to:
   - Exclude management endpoints from `http.server.requests`
   - Only include configured API paths

### Flow Diagram

```
HTTP Request → ReactiveContextWebFilter → Service Layer → Repository → Response
      ↓                    ↓                    ↓            ↓
   Context             Timer Start         Auto Context   Timer Stop
 Enrichment              + Tags            Propagation    + Recording
```

## Configuration

### Application Properties

Configure the metrics system in `application.yml`:

```yaml
weather:
  metrics:
    # Enable/disable custom reactive metrics collection
    enabled: true
    
    # Include detailed URI and method tags in metrics
    include-detailed-tags: true
    
    # API path prefixes to include in custom metrics (future-proof)
    included-api-paths:
      - "/api/v1/"
      # Future extensions:
      # - "/api/auth/"
      # - "/api/v2/"
      # - "/admin/api/"
```

### Future Extensions

To add new API endpoints for monitoring (no code changes required):

```yaml
weather:
  metrics:
    included-api-paths:
      - "/api/v1/"        # Weather API
      - "/api/auth/"      # Authentication API
      - "/api/v2/"        # Next version API
      - "/admin/api/"     # Admin API
      - "/internal/api/"  # Internal services
```

### Environment-Specific Configuration

```yaml
# Development - monitor everything
weather:
  metrics:
    enabled: true
    included-api-paths: ["/api/v1/", "/api/auth/", "/admin/"]

# Production - only public APIs
weather:
  metrics:
    enabled: true
    included-api-paths: ["/api/v1/"]

# Testing - disable custom metrics
weather:
  metrics:
    enabled: false
```

## Available Metrics

### Custom Reactive Metrics

| Metric Name | Type | Description | Tags |
|-------------|------|-------------|------|
| `reactive.operation.timer` | Timer | Request duration for API endpoints | `operation`, `result`, `uri`, `method` |
| `reactive.subscriptions.active` | Gauge | Currently active reactive subscriptions | - |
| `reactive.subscriptions.total` | Counter | Total subscriptions created | - |
| `reactive.subscription.started` | Counter | New subscriptions started | `operation` |
| `reactive.signal` | Counter | Signal types (complete/error/cancel) | `operation`, `type` |

### Built-in Spring Metrics (Filtered)

| Metric Name | Type | Description | Filtered To |
|-------------|------|-------------|-------------|
| `http.server.requests` | Timer | HTTP request metrics | Only configured API paths |
| `jvm.memory.used` | Gauge | JVM memory usage | All |
| `system.cpu.usage` | Gauge | System CPU usage | All |

### Tags Explained

- **`operation`**: Type of operation (`api_request`, `createWeatherData`, etc.)
- **`result`**: Outcome (`success`, `error`)
- **`uri`**: Request path (`/api/v1/weather`, `/api/v1/weather/city/{city}`)
- **`method`**: HTTP method (`GET`, `POST`, `PUT`, `DELETE`)
- **`type`**: Signal type (`complete`, `error`, `cancel`)

## Metrics Endpoints

### Base Metrics Endpoint

```bash
# List all available metrics
GET http://localhost:8080/management/metrics
```

### Custom Reactive Metrics

#### 1. All API Request Metrics
```bash
# Overview of all API request timing
GET http://localhost:8080/management/metrics/reactive.operation.timer?tag=operation:api_request

# Response includes available tags:
{
  "name": "reactive.operation.timer",
  "measurements": [
    {"statistic": "COUNT", "value": 42.0},
    {"statistic": "TOTAL_TIME", "value": 1.5},
    {"statistic": "MAX", "value": 0.25}
  ],
  "availableTags": [
    {"tag": "uri", "values": ["/api/v1/weather", "/api/v1/weather/city/{city}"]},
    {"tag": "method", "values": ["GET", "POST", "PUT", "DELETE"]},
    {"tag": "result", "values": ["success", "error"]}
  ]
}
```

#### 2. Endpoint-Specific Metrics
```bash
# Specific endpoint performance
GET http://localhost:8080/management/metrics/reactive.operation.timer?tag=operation:api_request&tag=uri:/api/v1/weather/city/{city}

# Method-specific performance  
GET http://localhost:8080/management/metrics/reactive.operation.timer?tag=operation:api_request&tag=method:GET

# Success vs error rates
GET http://localhost:8080/management/metrics/reactive.operation.timer?tag=operation:api_request&tag=result:success
GET http://localhost:8080/management/metrics/reactive.operation.timer?tag=operation:api_request&tag=result:error
```

#### 3. Combined Filters
```bash
# GET requests to weather by city - success only
GET http://localhost:8080/management/metrics/reactive.operation.timer?tag=operation:api_request&tag=uri:/api/v1/weather/city/{city}&tag=method:GET&tag=result:success

# All POST requests with errors
GET http://localhost:8080/management/metrics/reactive.operation.timer?tag=operation:api_request&tag=method:POST&tag=result:error
```

#### 4. Subscription Metrics
```bash
# Active reactive subscriptions
GET http://localhost:8080/management/metrics/reactive.subscriptions.active

# Total subscriptions created
GET http://localhost:8080/management/metrics/reactive.subscriptions.total

# Subscription start events
GET http://localhost:8080/management/metrics/reactive.subscription.started
```

#### 5. Signal Type Distribution
```bash
# All signal types
GET http://localhost:8080/management/metrics/reactive.signal

# Completion events only
GET http://localhost:8080/management/metrics/reactive.signal?tag=type:complete

# Error events only  
GET http://localhost:8080/management/metrics/reactive.signal?tag=type:error
```

### Built-in Spring Metrics (Filtered)

#### 1. HTTP Server Requests (API Only)
```bash
# All API endpoints (excludes management endpoints)
GET http://localhost:8080/management/metrics/http.server.requests

# Specific endpoint
GET http://localhost:8080/management/metrics/http.server.requests?tag=uri:/api/v1/weather/city/{city}

# Method-specific
GET http://localhost:8080/management/metrics/http.server.requests?tag=method:GET

# Status code filtering
GET http://localhost:8080/management/metrics/http.server.requests?tag=status:200
GET http://localhost:8080/management/metrics/http.server.requests?tag=status:404
```

#### 2. System Metrics
```bash
# JVM memory usage
GET http://localhost:8080/management/metrics/jvm.memory.used

# CPU usage
GET http://localhost:8080/management/metrics/system.cpu.usage

# Application uptime
GET http://localhost:8080/management/metrics/process.uptime
```

### Prometheus Format

```bash
# All metrics in Prometheus format (for monitoring tools)
GET http://localhost:8080/management/prometheus
```

## Context Propagation

### Automatic Context Enrichment

The `ReactiveContextWebFilter` automatically adds the following to the reactive context for all requests:

1. **Correlation ID**: Unique identifier for request tracing
2. **Request Timing**: Start timestamp for performance monitoring  
3. **User Context**: User information (anonymous by default)
4. **Request Metadata**: Path and HTTP method

### Context Flow Example

```java
@Service
public class WeatherDataServiceImpl {
    
    public Mono<WeatherDataResponse> createWeatherData(WeatherDataRequest request) {
        return validateRequest(request)
                .then(Mono.fromCallable(() -> mapper.toEntity(request)))
                .doOnNext(entity -> 
                    // Context automatically flows here - correlation ID available in logs
                    log.debug("Creating weather data for city: {}", entity.getCity())
                )
                .flatMap(repository::save)
                .map(mapper::toResponse)
                .doOnSuccess(response -> 
                    // Context with correlation ID still automatically available
                    log.info("Created weather data with id: {}", response.id())
                );
                // No manual context extraction needed!
    }
}
```

### Context Keys

| Key | Type | Description |
|-----|------|-------------|
| `correlationId` | String | Unique request identifier |
| `requestStartTime` | Long | Request start timestamp (ms) |
| `userContext` | UserContext | User information |
| `request.path` | String | Request path |
| `request.method` | String | HTTP method |

### Accessing Context (if needed)

```java
// In reactive chains, context is automatically available
return someReactiveOperation()
    .doOnNext(result -> {
        // Context flows automatically - no extraction needed
        log.info("Processing result: {}", result);
    })
    .contextWrite(context -> {
        // Manual context access (rarely needed)
        String correlationId = context.get("correlationId");
        return context;
    });
```

## Troubleshooting

### Common Issues

#### 1. No Custom Metrics Appearing

**Problem**: `reactive.operation.timer` metrics not showing up

**Solutions**:
```bash
# Check if metrics are enabled
curl http://localhost:8080/management/metrics | grep reactive

# Verify configuration
# In application.yml:
weather:
  metrics:
    enabled: true  # Must be true
    included-api-paths:
      - "/api/v1/"  # Must match your endpoint paths
```

#### 2. Too Many Metrics (Including Management Endpoints)

**Problem**: Metrics include `/management/*` endpoints

**Solutions**:
- Verify `MetricsFilterConfig` is properly excluding management endpoints
- Check that `included-api-paths` doesn't include management paths
- Use filtered endpoints: `?tag=operation:api_request`

#### 3. Missing URI/Method Tags

**Problem**: Metrics don't show URI or method breakdown

**Solutions**:
```yaml
weather:
  metrics:
    include-detailed-tags: true  # Must be true for detailed tags
```

#### 4. Context Not Propagating

**Problem**: Correlation IDs not appearing in logs

**Solutions**:
- Verify `ReactiveContextWebFilter` is registered as a Spring component
- Check that reactive chains use proper operators (`doOnNext`, `flatMap`, etc.)
- Avoid blocking operations that break the reactive chain

### Health Checks

```bash
# Verify metrics endpoint is accessible
curl http://localhost:8080/management/health

# Check metrics configuration
curl http://localhost:8080/management/info

# Test specific API endpoint to generate metrics
curl http://localhost:8080/api/v1/weather/cities

# Verify metrics were recorded
curl "http://localhost:8080/management/metrics/reactive.operation.timer?tag=operation:api_request"
```

### Logging

To debug metrics collection, enable debug logging:

```yaml
logging:
  level:
    com.weather.config: DEBUG
    io.micrometer: DEBUG
```

## Best Practices

1. **Keep API paths specific**: Use `/api/v1/` instead of `/api/` to avoid too broad matching
2. **Monitor key endpoints**: Focus on business-critical API paths
3. **Use environment-specific config**: Different monitoring needs for dev/prod
4. **Regular metrics review**: Check for performance bottlenecks and error patterns
5. **Correlation ID tracing**: Use correlation IDs for distributed tracing
6. **Avoid blocking in reactive chains**: Maintain proper reactive flow for context propagation

## Integration with Monitoring Tools

### Prometheus + Grafana

```bash
# Prometheus scraping endpoint
GET http://localhost:8080/management/prometheus

# Example Grafana queries:
# - API request rate: rate(reactive_operation_timer_total{operation="api_request"}[5m])
# - Error rate: rate(reactive_operation_timer_total{operation="api_request",result="error"}[5m])
# - Average response time: reactive_operation_timer_total{operation="api_request"} / reactive_operation_timer_count{operation="api_request"}
```

### Custom Dashboards

Create dashboards focusing on:
- Request volume by endpoint
- Response time percentiles
- Error rates by endpoint/method
- Active subscription counts
- Context propagation success rates
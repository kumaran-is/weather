# 🚀 **Netty Configuration Guide for Spring WebFlux**

## **Table of Contents**

1. [What is Netty?](#what-is-netty)
2. [Why Netty in Spring WebFlux?](#why-netty-in-spring-webflux)
3. [Configuration Overview](#configuration-overview)
4. [Environment-Specific Settings](#environment-specific-settings)
5. [Key Configuration Parameters](#key-configuration-parameters)
6. [Performance Tuning](#performance-tuning)
7. [Best Practices](#best-practices)
8. [Monitoring & Observability](#monitoring--observability)
9. [Troubleshooting](#troubleshooting)
10. [Advanced Configuration](#advanced-configuration)
11. [Security Considerations](#security-considerations)
12. [References](#references)

---

## **What is Netty?**

**Netty** is a high-performance, asynchronous event-driven network application framework that enables rapid development of maintainable high-performance protocol servers and clients. It's the default embedded server for Spring WebFlux applications.

### **🎯 Core Concepts:**
- **Event-Driven Architecture**: Non-blocking I/O operations
- **Channel Pipeline**: Request processing pipeline with handlers
- **Event Loop**: Single-threaded event processing model
- **ByteBuf**: Efficient buffer management for network data
- **Bootstrap**: Server and client configuration and startup

### **📊 Netty vs Traditional Servers:**

| Aspect | Traditional (Tomcat) | Netty |
|--------|---------------------|--------|
| **Threading Model** | Thread-per-request | Event-loop based |
| **I/O Model** | Blocking I/O | Non-blocking I/O |
| **Memory Usage** | Higher (thread stacks) | Lower (shared event loops) |
| **Scalability** | Limited by threads | High concurrency |
| **Best For** | Traditional web apps | Reactive applications |

---

## **Why Netty in Spring WebFlux?**

### **🚀 Performance Benefits**

1. **High Concurrency**: Handles thousands of concurrent connections with minimal threads
2. **Low Latency**: Non-blocking I/O reduces response times
3. **Memory Efficiency**: Shared event loops vs thread-per-request model
4. **Backpressure Support**: Natural integration with reactive streams

### **💡 Reactive Programming Alignment**

```java
// Traditional blocking approach
@RestController
public class BlockingController {
    public ResponseEntity<String> getData() {
        String data = blockingDatabaseCall(); // Blocks thread
        return ResponseEntity.ok(data);
    }
}

// Reactive approach with Netty
@RestController
public class ReactiveController {
    public Mono<ResponseEntity<String>> getData() {
        return databaseService.getData()  // Non-blocking
            .map(ResponseEntity::ok);     // Continues on event loop
    }
}
```

### **🏆 Business Impact**
- **Cost Reduction**: Fewer server resources needed
- **Better User Experience**: Lower response times
- **Improved Scalability**: Handle more users per server
- **Resource Optimization**: Better CPU and memory utilization

---

## **Configuration Overview**

### **📁 Configuration Structure**

The Weather Service uses environment-specific Netty configurations:

```
src/main/resources/
├── application.yml           # Base Netty configuration
├── application-local.yml     # Local development settings
├── application-dev.yml       # Development environment
├── application-qa.yml        # QA testing environment
└── application-prod.yml      # Production optimized settings
```

### **⚙️ Configuration Hierarchy**

1. **Base Configuration** (`application.yml`): Default settings for all environments
2. **Environment Overrides**: Profile-specific optimizations
3. **Runtime Properties**: Environment variables and system properties

---

## **Environment-Specific Settings**

### **🌍 Configuration Matrix**

| Environment | Use Case | Connection Timeout | Idle Timeout | Max Content | Header Validation |
|-------------|----------|-------------------|--------------|-------------|-------------------|
| **Local** | Single developer | 2s | 60s | 2MB | Disabled |
| **Dev** | Team development | 3s | 120s | 4MB | Enabled |
| **QA** | Load testing | 4s | 180s | 6MB | Enabled |
| **Production** | Live traffic | 5s | 300s | 8MB | Enabled |

### **📋 Detailed Environment Configurations**

#### **Local Development (`application-local.yml`)**
```yaml
server:
  netty:
    connection-timeout: 2s          # Fast feedback for local dev
    idle-timeout: 60s               # Quick cleanup of idle connections
    max-initial-line-length: 4096   # 4KB - sufficient for dev requests
    h2c-max-content-length: 2097152 # 2MB - small payloads for testing
    validate-headers: false         # Disabled for faster development
```

**📝 Purpose**: Optimized for rapid development cycles and single-user debugging.

#### **Development Environment (`application-dev.yml`)**
```yaml
server:
  netty:
    connection-timeout: 3s          # Moderate timeout for team dev
    idle-timeout: 120s              # 2 minutes for shared development
    max-initial-line-length: 6144   # 6KB - larger dev/test requests
    h2c-max-content-length: 4194304 # 4MB - medium payloads for testing
    validate-headers: true          # Enable validation for debugging
```

**📝 Purpose**: Balanced configuration for team development and integration testing.

#### **QA Environment (`application-qa.yml`)**
```yaml
server:
  netty:
    connection-timeout: 4s          # Extended timeout for load testing
    idle-timeout: 180s              # 3 minutes for test scenarios
    max-initial-line-length: 6144   # 6KB - test request handling
    h2c-max-content-length: 6291456 # 6MB - larger test payloads
    validate-headers: true          # Always validate in QA
```

**📝 Purpose**: Configured for load testing, performance validation, and pre-production verification.

#### **Production Environment (`application-prod.yml`)**
```yaml
server:
  netty:
    connection-timeout: 5s          # Extended for production networks
    idle-timeout: 300s              # 5 minutes - optimize connection reuse
    max-initial-line-length: 8192   # 8KB - handle larger production requests
    h2c-max-content-length: 8388608 # 8MB - production payload sizes
    validate-headers: true          # Always validate for security
```

**📝 Purpose**: Optimized for high throughput, security, and production stability.

---

## **Key Configuration Parameters**

### **🔧 Connection Management**

#### **Connection Timeout**
```yaml
server:
  netty:
    connection-timeout: 5s
```

| Parameter | Purpose | Recommended Values |
|-----------|---------|-------------------|
| **Local** | Fast feedback | 2s |
| **Dev/QA** | Moderate tolerance | 3-4s |
| **Production** | Network tolerance | 5s+ |

**💡 Impact**: Controls how long to wait for TCP connection establishment.

#### **Idle Timeout**
```yaml
server:
  netty:
    idle-timeout: 300s
```

| Environment | Value | Reasoning |
|-------------|-------|-----------|
| **Local** | 60s | Quick cleanup for dev |
| **Dev** | 120s | Moderate for debugging |
| **QA** | 180s | Extended for test scenarios |
| **Production** | 300s | Optimize connection reuse |

**💡 Impact**: Prevents resource leaks from abandoned connections.

### **📦 Content and Buffer Settings**

#### **Max Initial Line Length**
```yaml
server:
  netty:
    max-initial-line-length: 8192
```

**📝 Purpose**: Controls the maximum length of the HTTP request line (method, URI, version).

| Size | Use Case | Security Impact |
|------|----------|----------------|
| 4KB | Development | Low attack surface |
| 6KB | Testing | Moderate protection |
| 8KB | Production | Balanced security/functionality |

#### **H2C Max Content Length**
```yaml
server:
  netty:
    h2c-max-content-length: 8388608  # 8MB
```

**📝 Purpose**: Maximum content length for HTTP/2 cleartext connections.

| Size | Environment | Use Case |
|------|-------------|----------|
| 2MB | Local | Small dev payloads |
| 4-6MB | Dev/QA | Medium test data |
| 8MB+ | Production | Large file uploads |

### **🔒 Security Settings**

#### **Header Validation**
```yaml
server:
  netty:
    validate-headers: true
```

| Setting | Environment | Security Level |
|---------|-------------|----------------|
| `false` | Local only | Development speed |
| `true` | Dev/QA/Prod | Production security |

**⚠️ Security Note**: Always enable in non-local environments to prevent header injection attacks.

---

## **Performance Tuning**

### **🎯 Optimization Strategies**

#### **1. Connection Pool Optimization**

```yaml
# Optimize for high-concurrency scenarios
server:
  netty:
    connection-timeout: 5s      # Network tolerance
    idle-timeout: 300s          # Connection reuse
```

#### **2. Buffer Size Tuning**

```yaml
# Balance memory usage vs functionality
server:
  netty:
    max-initial-line-length: 8192    # 8KB initial line
    h2c-max-content-length: 8388608  # 8MB content
```

#### **3. Environment-Specific Tuning**

| Metric | Local | Dev | QA | Production |
|--------|-------|-----|----|---------  |
| **Throughput Priority** | Low | Medium | High | Highest |
| **Memory Usage** | Minimal | Low | Medium | Optimized |
| **Security Level** | Basic | Medium | High | Maximum |

### **📊 Performance Monitoring**

#### **Key Metrics to Track**

```yaml
# Enable Netty metrics in application.yml
management:
  endpoints:
    web:
      exposure:
        include: metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

**📈 Important Netty Metrics:**
- `netty_pool_used_connections`
- `netty_pool_pending_connections`
- `netty_pool_max_connections`
- `netty_pool_active_connections`

---

## **Best Practices**

### **✅ Configuration Best Practices**

#### **1. Environment Progression**
```yaml
# Gradual increase in limits across environments
Local (2s) → Dev (3s) → QA (4s) → Production (5s)
```

#### **2. Security Hardening**
```yaml
# Always validate headers in higher environments
server:
  netty:
    validate-headers: true  # QA and Production
    validate-headers: false # Local only for speed
```

#### **3. Resource Management**
```yaml
# Appropriate content limits per environment
server:
  netty:
    h2c-max-content-length: 2097152  # Local: 2MB
    h2c-max-content-length: 8388608  # Production: 8MB
```

### **🚫 Common Anti-Patterns**

| ❌ Anti-Pattern | ✅ Best Practice | Impact |
|----------------|-----------------|--------|
| Same config for all envs | Environment-specific tuning | Performance/Security |
| Unlimited content length | Appropriate size limits | DoS protection |
| Disabled validation in prod | Always validate in prod | Security vulnerabilities |
| Very long timeouts | Environment-appropriate timeouts | Resource efficiency |

### **📋 Configuration Checklist**

- [ ] **Environment-specific settings** configured
- [ ] **Security validation** enabled in prod
- [ ] **Appropriate timeouts** set per environment
- [ ] **Content length limits** configured
- [ ] **Monitoring** enabled for Netty metrics
- [ ] **Header validation** enabled (except local)
- [ ] **Performance testing** completed for QA/Prod settings

---

## **Monitoring & Observability**

### **📊 Netty Metrics Integration**

#### **Prometheus Metrics**
```yaml
# Enable detailed Netty metrics
management:
  metrics:
    tags:
      service: weather-service
      environment: ${ENVIRONMENT:local}
    export:
      prometheus:
        enabled: true
        descriptions: true
```

#### **Key Monitoring Points**

| Metric Category | Metrics | Purpose |
|----------------|---------|---------|
| **Connections** | `netty_pool_*` | Connection pool health |
| **Performance** | `http_server_requests_*` | Request performance |
| **Errors** | `netty_pool_failed_*` | Connection failures |
| **Resources** | `jvm_memory_used_bytes` | Memory utilization |

### **🚨 Alerting Recommendations**

#### **Critical Alerts**
```yaml
# Connection pool exhaustion
alert: netty_pool_used_connections / netty_pool_max_connections > 0.9

# High connection failure rate
alert: rate(netty_pool_failed_connections[5m]) > 10

# Extended response times
alert: histogram_quantile(0.95, http_server_requests_duration_seconds) > 1.0
```

### **📈 Grafana Dashboard Queries**

#### **Connection Pool Utilization**
```promql
# Connection pool usage percentage
(netty_pool_used_connections / netty_pool_max_connections) * 100
```

#### **Request Duration by Environment**
```promql
# 95th percentile response time by environment
histogram_quantile(0.95, 
  rate(http_server_requests_duration_seconds_bucket[5m])
) * 1000
```

---

## **Troubleshooting**

### **🔍 Common Issues & Solutions**

#### **1. Connection Timeout Errors**

**❌ Symptom:**
```
java.util.concurrent.TimeoutException: connection timed out
```

**✅ Solution:**
```yaml
# Increase connection timeout
server:
  netty:
    connection-timeout: 10s  # Increase from default
```

#### **2. Request Too Large Errors**

**❌ Symptom:**
```
io.netty.handler.codec.TooLongFrameException: content length exceeded
```

**✅ Solution:**
```yaml
# Increase content length limit
server:
  netty:
    h2c-max-content-length: 16777216  # 16MB
```

#### **3. Header Validation Failures**

**❌ Symptom:**
```
io.netty.handler.codec.http.HttpHeaderValidationException
```

**✅ Solution:**
```yaml
# Disable validation (local only) or fix client headers
server:
  netty:
    validate-headers: false  # Local development only
```

#### **4. Connection Pool Exhaustion**

**❌ Symptom:**
```
reactor.netty.internal.PooledConnectionProvider: connection pool exhausted
```

**✅ Solutions:**
1. **Check R2DBC pool settings**
2. **Monitor connection leaks**
3. **Tune idle timeouts**

### **🛠️ Debugging Tools**

#### **1. Enable Debug Logging**
```yaml
logging:
  level:
    reactor.netty: DEBUG
    io.netty: DEBUG
```

#### **2. JVM Monitoring**
```bash
# Monitor Netty threads
jstack <pid> | grep -A 5 -B 5 "netty"

# Monitor memory usage
jstat -gc <pid> 1s
```

#### **3. Network Analysis**
```bash
# Monitor network connections
netstat -an | grep :8080

# Check connection states
ss -tuln | grep :8080
```

---

## **Advanced Configuration**

### **⚡ High-Performance Tuning**

#### **Native Transport (Linux)**
```yaml
# Enable native transport for better performance (Linux only)
server:
  netty:
    # These are JVM args, not YAML config
    # -Dio.netty.transport.noNative=false
    # -Dio.netty.transport.epoll.maxEventsAtOnce=1024
```

#### **Custom Thread Configuration**
```java
// Custom Netty configuration bean
@Configuration
public class NettyCustomConfiguration {
    
    @Bean
    public NettyReactiveWebServerFactory nettyReactiveWebServerFactory() {
        NettyReactiveWebServerFactory factory = new NettyReactiveWebServerFactory();
        factory.addServerCustomizers(httpServer -> 
            httpServer.runOn(LoopResources.create("weather-http", 8, true))
        );
        return factory;
    }
}
```

### **🔒 Advanced Security Configuration**

#### **SSL/TLS Configuration**
```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
  netty:
    # SSL-specific settings
    validate-headers: true
    max-initial-line-length: 4096  # Smaller for security
```

#### **Rate Limiting Integration**
```java
// Custom rate limiting filter
@Component
public class NettyRateLimitFilter implements WebFilter {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Implement rate limiting logic
        return rateLimiter.isAllowed(getClientId(exchange))
            .flatMap(allowed -> allowed ? 
                chain.filter(exchange) : 
                handleRateLimit(exchange)
            );
    }
}
```

---

## **Security Considerations**

### **🛡️ Security Configuration Matrix**

| Setting | Local | Dev | QA | Production | Security Impact |
|---------|-------|-----|----|-----------| --------------- |
| `validate-headers` | false | true | true | true | Header injection prevention |
| `max-content-length` | 2MB | 4MB | 6MB | 8MB | DoS protection |
| `connection-timeout` | 2s | 3s | 4s | 5s | Resource exhaustion |
| `idle-timeout` | 60s | 120s | 180s | 300s | Connection cleanup |

### **⚠️ Security Best Practices**

#### **1. Content Length Limits**
```yaml
# Always set appropriate limits
server:
  netty:
    h2c-max-content-length: 8388608  # 8MB max
    max-initial-line-length: 8192    # 8KB max request line
```

**📝 Purpose**: Prevents denial-of-service attacks through large payloads.

#### **2. Header Validation**
```yaml
# Enable in all non-local environments
server:
  netty:
    validate-headers: true
```

**📝 Purpose**: Prevents HTTP header injection and malformed request attacks.

#### **3. Timeout Configuration**
```yaml
# Prevent resource exhaustion
server:
  netty:
    connection-timeout: 5s  # Reasonable limit
    idle-timeout: 300s      # Cleanup abandoned connections
```

**📝 Purpose**: Prevents connection exhaustion attacks.

### **🔍 Security Monitoring**

#### **Security Metrics to Track**
- Failed connection attempts
- Oversized request rejections
- Header validation failures
- Connection timeout rates

#### **Security Alerts**
```yaml
# High rejection rate alert
alert: rate(http_requests_rejected_total[5m]) > 100

# Header validation failures
alert: rate(netty_header_validation_failed[5m]) > 10
```

---

## **References**

### **📚 Documentation**
- [Spring Boot Netty Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/web.html#web.reactive.server.configuration)
- [Netty Official Documentation](https://netty.io/wiki/index.html)
- [Project Reactor Netty](https://projectreactor.io/docs/netty/release/reference/index.html)

### **🔗 Related Weather Service Documentation**
- [BlockHound Integration Guide](./BLOCKHOUND-GUIDE.md)
- [Java 21 Spring Reactive Checklist](./checklist/Java21-SpringReactiveChecklist.md)
- [Architecture Decision Records](./architecture/decisions/)

### **🛠️ Tools and Utilities**
- [Netty Performance Testing](https://github.com/netty/netty/tree/4.1/microbench)
- [Spring Boot Actuator Metrics](https://docs.spring.io/spring-boot/docs/current/actuator-api/htmlsingle/)
- [Micrometer Prometheus Integration](https://micrometer.io/docs/registry/prometheus)

### **📊 Performance Benchmarks**
- [Netty vs Tomcat Performance Comparison](https://www.techempower.com/benchmarks/)
- [Spring WebFlux Performance Guide](https://spring.io/guides/gs/reactive-rest-service/)

---

## **Conclusion**

This Netty configuration guide provides comprehensive settings for optimal performance across all environments. The configurations balance performance, security, and resource utilization while maintaining the reactive nature of the Weather Service application.

**Key Takeaways:**
- ✅ Environment-specific configurations optimize for each use case
- ✅ Security settings are hardened for production environments
- ✅ Performance monitoring provides visibility into Netty operations
- ✅ Best practices prevent common configuration issues
- ✅ Troubleshooting guide helps resolve operational problems

For questions or issues, refer to the troubleshooting section or consult the Spring Boot and Netty documentation.
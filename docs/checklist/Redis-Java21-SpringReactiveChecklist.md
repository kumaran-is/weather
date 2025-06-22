# ✅ **Redis Cache Integration Checklist for Java 21 + Spring WebFlux 3.4.5**

## 📑 Table of Contents
- [1. Dependency Setup](#1-dependency-setup)
- [2. Configuration (application.yml/properties)](#2-configuration-applicationymlproperties)
- [3. Bean Configuration](#3-bean-configuration)
- [4. Reactive Usage (Service Layer)](#4-reactive-usage-service-layer)
- [5. Cache Population Strategy](#5-cache-population-strategy)
- [6. Error Handling & Resilience](#6-error-handling--resilience)
- [7. Key Management Best Practices](#7-key-management-best-practices)
- [8. Testing](#8-testing)
- [9. Observability & Monitoring](#9-observability--monitoring)
- [10. Common Pitfalls to Avoid](#10-common-pitfalls-to-avoid)
- [Bonus: Optional Enhancements](#bonus-optional-enhancements)
- [Java 21 Specific Enhancements](#java-21-specific-enhancements)

Here's a tailored **checklist for integrating Redis Cache in a fully reactive Spring Boot WebFlux 3.4.5 REST application using Java 21**, ensuring you maintain **non-blocking**, **reactive end-to-end flow**, and follow **modern caching best practices**.

### 📦 **1. Dependency Setup**

* [ ] Use **Reactive Redis dependency**, not the traditional blocking `Jedis` or `Lettuce` sync APIs:

  ```xml
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
  </dependency>
  ```

* [ ] Add **Redis connection pool optimization** dependency for better performance:
  ```xml
  <dependency>
    <groupId>io.lettuce.core</groupId>
    <artifactId>lettuce-core</artifactId>
  </dependency>
  ```

* [ ] Include **Jackson modules for Java 21** features:
  ```xml
  <dependency>
    <groupId>com.fasterxml.jackson.module</groupId>
    <artifactId>jackson-module-parameter-names</artifactId>
  </dependency>
  ```

---

### ⚙️ **2. Configuration (application.yml/properties)**

* [ ] Set up Redis host/port, connection pool, and timeout settings:

  ```yaml
  spring:
    data:
      redis:
        host: localhost
        port: 6379
        timeout: 5000
  ```

* [ ] Configure **Lettuce connection pool** for optimal performance:
  ```yaml
  spring:
    data:
      redis:
        lettuce:
          pool:
            max-active: 8
            max-idle: 8
            min-idle: 2
            max-wait: -1ms
        timeout: 2000ms
        connect-timeout: 5000ms
        command-timeout: 5000ms
  ```

* [ ] Enable **Redis Cluster support** if using clustered Redis:
  ```yaml
  spring:
    data:
      redis:
        cluster:
          nodes: 
            - 127.0.0.1:7000
            - 127.0.0.1:7001
          max-redirects: 3
  ```

* [ ] Use **Lettuce** (default) for reactive Redis connection:

  * It is non-blocking and compatible with Project Reactor.

---

### 🧩 **3. Bean Configuration**

* [ ] Define `ReactiveRedisConnectionFactory` and `ReactiveRedisTemplate` manually **if customization needed** (e.g., custom serializers):

  ```java
  @Bean
  public ReactiveRedisTemplate<String, YourDTO> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
      RedisSerializationContext<String, YourDTO> context = RedisSerializationContext
          .<String, YourDTO>newSerializationContext(new StringRedisSerializer())
          .value(new Jackson2JsonRedisSerializer<>(YourDTO.class))
          .build();
      return new ReactiveRedisTemplate<>(factory, context);
  }
  ```

* [ ] Configure **custom JSON serialization** for Java 21 records and sealed classes:
  ```java
  @Bean
  public Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer() {
      Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
      ObjectMapper mapper = new ObjectMapper();
      mapper.registerModule(new JavaTimeModule());
      mapper.registerModule(new ParameterNamesModule());
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      serializer.setObjectMapper(mapper);
      return serializer;
  }
  ```

* [ ] Create **Redis health indicator** for better monitoring:
  ```java
  @Bean
  public ReactiveHealthIndicator redisHealthIndicator(ReactiveRedisTemplate<String, Object> template) {
      return new ReactiveRedisHealthIndicator(template.getConnectionFactory());
  }
  ```

* [ ] If using `ObjectMapper`, ensure it's **properly configured** (modules, time zones, etc.)

---

### ⚛️ **4. Reactive Usage (Service Layer)**

* [ ] Always use **`reactiveRedisTemplate.opsForValue().get(key)`** or `.set(key, value)` which return `Mono<T>`.

* [ ] Chain `.flatMap()`, `.map()`, `.switchIfEmpty()` etc. after cache fetch in your service:

  ```java
  return reactiveRedisTemplate.opsForValue().get(key)
      .switchIfEmpty(fetchFromDbAndCache(key));
  ```

* [ ] Implement **batch operations** for better performance:
  ```java
  public Mono<List<YourDTO>> getMultiple(List<String> keys) {
      return reactiveRedisTemplate.opsForValue().multiGet(keys);
  }
  ```

* [ ] Use **pipeline operations** for multiple Redis commands:
  ```java
  public Mono<List<Object>> executePipeline(List<String> keys) {
      return reactiveRedisTemplate.execute(connection -> 
          connection.closePipeline().flux().collectList());
  }
  ```

* [ ] NEVER call `.block()` or `.subscribe()` – keep the pipeline reactive.

---

### 🔁 **5. Cache Population Strategy**

* [ ] Use `switchIfEmpty(...)` to populate cache lazily:

  ```java
  Mono<YourDTO> fetchFromDbAndCache(String key) {
      return dbService.getFromDB()
          .flatMap(value -> reactiveRedisTemplate.opsForValue().set(key, value)
              .thenReturn(value));
  }
  ```

* [ ] Use **cache-aside pattern** with proper null handling:
  ```java
  return reactiveRedisTemplate.opsForValue().get(key)
      .cast(YourDTO.class)
      .switchIfEmpty(loadFromSourceAndCache(key))
      .onErrorResume(throwable -> loadFromSource(key));
  ```

* [ ] Implement **cache warming** strategy on application startup:
  ```java
  @EventListener(ApplicationReadyEvent.class)
  public void warmUpCache() {
      // Preload critical data
  }
  ```

* [ ] Use `TTL` to prevent stale cache entries:

  ```java
  opsForValue().set(key, value, Duration.ofMinutes(30));
  ```

---

### 🛡️ **6. Error Handling & Resilience**

* [ ] Wrap Redis calls with `.onErrorResume()` to fail gracefully:

  ```java
  reactiveRedisTemplate.opsForValue().get(key)
      .onErrorResume(ex -> {
          log.warn("Redis error: ", ex);
          return Mono.empty(); // fallback logic
      });
  ```

* [ ] Implement **circuit breaker pattern** for Redis failures:
  ```java
  @Component
  public class RedisCircuitBreaker {
      private final CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("redis");
      
      public <T> Mono<T> execute(Supplier<Mono<T>> supplier) {
          return Mono.fromSupplier(CircuitBreaker.decorateSupplier(circuitBreaker, supplier))
                     .flatMap(mono -> mono);
      }
  }
  ```

* [ ] Add **retry mechanism** with exponential backoff:
  ```java
  .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
  ```

* [ ] Avoid letting Redis downtime crash your application. Treat it as a **best-effort cache**.

---

### 🎯 **7. Key Management Best Practices**

* [ ] Use **namespacing** for keys (e.g., `user::123`, `product::sku123`) to avoid collisions.
* [ ] Normalize keys to lowercase or specific casing consistently.
* [ ] Avoid overly long keys or large payloads in cache (>1MB).
* [ ] Implement **key expiration events** handling:
  ```java
  @EventListener
  public void handleKeyExpiration(RedisKeyExpiredEvent event) {
      // Handle expired key logic
  }
  ```

* [ ] Use **consistent hashing** for distributed cache keys:
  ```java
  private String generateConsistentKey(String baseKey) {
      return DigestUtils.md5Hex(baseKey) + "::" + baseKey;
  }
  ```

---

### 🧪 **8. Testing**

* [ ] Use **embedded Redis** (e.g., `embedded-redis` test dependency) or mock `ReactiveRedisTemplate` in unit tests.
* [ ] Use **Testcontainers** for integration testing with real Redis:
  ```java
  @TestMethodOrder(OrderAnnotation.class)
  @Testcontainers
  class RedisIntegrationTest {
      @Container
      static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
              .withExposedPorts(6379);
  }
  ```

* [ ] Create **reactive test scenarios** with `StepVerifier`:
  ```java
  StepVerifier.create(cacheService.get("key"))
      .expectNext(expectedValue)
      .verifyComplete();
  ```

* [ ] Write tests for both cache-hit and cache-miss paths.
* [ ] Verify TTLs and value serialization integrity.

---

### 📊 **9. Observability & Monitoring**

* [ ] Log Redis errors and slow command execution using `.doOnError()`, `.doOnNext()`, `.log()`.
* [ ] Add **custom metrics** for cache hit/miss ratios:
  ```java
  @Component
  public class CacheMetrics {
      private final Counter cacheHits = Counter.builder("cache.hits").register(meterRegistry);
      private final Counter cacheMisses = Counter.builder("cache.misses").register(meterRegistry);
  }
  ```

* [ ] Implement **distributed tracing** with Micrometer:
  ```java
  return Mono.just(key)
      .name("redis.get")
      .tag("operation", "get")
      .metrics();
  ```

* [ ] Expose Redis metrics via **Micrometer** and integrate with **Prometheus + Grafana**.
* [ ] Use Spring Boot actuator to monitor Redis health (`/actuator/health`).

---

### ⚠️ **10. Common Pitfalls to Avoid**

#### **General Reactive & WebFlux Don'ts:**
* [ ] ❌ Don't use `RestTemplate` or JDBC inside cache service.
* [ ] ❌ Don't put blocking data sources inside `.flatMap()`.
* [ ] ❌ Don't mix `@Cacheable` (Spring Cache) with reactive code unless it supports `Mono`/`Flux`.
* [ ] ❌ Don't call `.block()` or `.blockFirst()` in WebFlux controllers or services
* [ ] ❌ Don't use `ThreadLocal` variables in reactive pipelines
* [ ] ❌ Don't ignore backpressure - always handle `Flux` overflow scenarios

#### **Redis-Specific Don'ts:**
* [ ] ❌ Don't serialize **large objects** (>10MB) directly to Redis
* [ ] ❌ Don't use **blocking Redis operations** in WebFlux controllers
* [ ] ❌ Don't ignore **memory usage patterns** - monitor Redis memory consumption
* [ ] ❌ Don't forget to handle **Redis AUTH** in production environments
* [ ] ❌ Don't store sensitive data (passwords, tokens) in Redis without encryption
* [ ] ❌ Don't use Redis as primary data store - treat it as cache only
* [ ] ❌ Don't ignore Redis key expiration - always set TTL for cache entries
* [ ] ❌ Don't use overly complex Redis data structures for simple caching
* [ ] ❌ Don't perform multiple individual Redis calls when batch operations are available
* [ ] ❌ Don't ignore Redis connection limits - monitor active connections

#### **Java 21 + Spring WebFlux Specific Don'ts:**
* [ ] ❌ Don't mix Virtual Threads with reactive streams unnecessarily
* [ ] ❌ Don't use blocking I/O operations inside reactive operators
* [ ] ❌ Don't ignore proper exception handling in reactive chains
* [ ] ❌ Don't forget to configure proper serialization for Java Records in Redis
* [ ] ❌ Don't use reflection-heavy operations in hot cache paths
* [ ] ❌ Don't ignore proper resource cleanup in reactive Redis connections

#### **Performance & Production Don'ts:**
* [ ] ❌ Don't deploy without proper Redis monitoring and alerting
* [ ] ❌ Don't ignore Redis memory fragmentation issues
* [ ] ❌ Don't use default Redis configurations in production
* [ ] ❌ Don't forget to implement proper cache warming strategies
* [ ] ❌ Don't ignore Redis slow query logs
* [ ] ❌ Don't use single Redis instance for high-availability requirements
* [ ] ❌ Don't forget to implement graceful degradation when Redis is unavailable

#### **Security Don'ts:**
* [ ] ❌ Don't expose Redis ports directly to the internet
* [ ] ❌ Don't use Redis without authentication in production
* [ ] ❌ Don't store unencrypted PII data in Redis cache
* [ ] ❌ Don't ignore Redis security updates and patches
* [ ] ❌ Don't use weak Redis passwords or default credentials

---

### ✅ **Bonus: Optional Enhancements**

* [ ] Use **LRU/LFU eviction policies** in Redis config.
* [ ] Support **cache invalidation** (e.g., on update/delete) by explicitly removing keys:

  ```java
  reactiveRedisTemplate.delete(key);
  ```
* [ ] Implement **Pub/Sub** with `reactiveRedisTemplate.listenToChannel(...)` for cache syncing across nodes.

---

### 🚀 **Java 21 Specific Enhancements**

* [ ] Use **Virtual Threads** for Redis connection handling (if needed):
  ```java
  @Bean
  public Executor virtualThreadExecutor() {
      return Executors.newVirtualThreadPerTaskExecutor();
  }
  ```

* [ ] Leverage **Pattern Matching** for cache value processing:
  ```java
  return switch (cacheValue) {
      case String s -> processString(s);
      case Number n -> processNumber(n);
      case null -> Mono.empty();
      default -> processDefault(cacheValue);
  };
  ```

* [ ] Use **Records** for cache DTOs with proper serialization:
  ```java
  public record CacheableDTO(String id, String name, @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date) {}
  ```

---
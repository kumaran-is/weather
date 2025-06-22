# ✅ **Java 21 + Spring WebFlux Reactive REST Checklist**

## 📑 Table of Contents
- [General Reactive Programming Principles](#general-reactive-programming-principles)
- [Java 21 Specifics](#java-21-specifics)
- [Spring WebFlux Controller Layer](#spring-webflux-controller-layer)
- [Data Access Layer](#data-access-layer)
- [WebClient for External Calls](#webclient-for-external-calls)
- [Threading & Schedulers](#threading--schedulers)
- [Configuration & Properties](#configuration--properties)
- [Performance & Optimization](#performance--optimization)
- [Testing & Debugging](#testing--debugging)
- [BlockHound Integration](#blockhound-integration)
- [Monitoring & Observability](#monitoring--observability)
- [Security & Resilience](#security--resilience)
- [Clean Code & Design](#clean-code--design)
- [Project Setup](#project-setup)
- [Pro-Tips](#pro-tips)
- [DON'T Checklist - Common Anti-Patterns to Avoid](#dont-checklist---common-anti-patterns-to-avoid)

### 🔁 **General Reactive Programming Principles**

* [ ] **Avoid blocking calls** – never use `Thread.sleep()`, `InputStream`, `BlockingQueue`, `jdbcTemplate`, etc.
* [ ] **Use `Mono` and `Flux` everywhere** – always return `Mono<T>` or `Flux<T>` in service, repo, controller layers.
* [ ] **Chain operators properly** – use `.map()`, `.flatMap()`, `.switchIfEmpty()` etc. to transform or chain logic.
* [ ] **Propagate backpressure** – design `Flux` chains to support consumer's demand using reactive backpressure.
* [ ] **Avoid side effects in reactive chains** – don't mutate shared state inside `map`/`flatMap`.
* [ ] **Understand hot vs cold publishers** – ensure proper subscription semantics and avoid unintended data replays.
* [ ] **Use `flatMapSequential()` when order matters** – preserves order while maintaining concurrency.
* [ ] **Implement proper error handling** – use `.onErrorResume()`, `.onErrorReturn()`, `.onErrorContinue()` appropriately.

---

### 🔧 **Java 21 Specifics**

* [ ] Prefer **virtual threads for blocking tasks**, but **keep them isolated** and separate from reactive pipeline.
* [ ] Avoid mixing **Project Loom (virtual threads)** with **reactive stacks** unless you know the boundary clearly.
* [ ] Use Java 21 **records** for immutable DTOs to simplify code and ensure thread-safety.
* [ ] Leverage **pattern matching** and **switch expressions** for cleaner reactive transformations.
* [ ] Use **sequenced collections** for predictable ordering in reactive chains.

---

### ⚙️ **Spring WebFlux Controller Layer**

* [ ] Use `@RestController` + `@RequestMapping` with return types as `Mono<T>` or `Flux<T>`.
* [ ] Never use `@ResponseBody` methods returning `T` (blocking) in reactive controllers.
* [ ] Avoid `@RequestParam MultipartFile` – instead use `Part` from `org.springframework.http.codec.multipart`.
* [ ] Use `ServerResponse` and `RouterFunction` for functional endpoints if you prefer functional over annotated style.
* [ ] **Implement proper request validation** using `@Valid` with reactive error handling.
* [ ] **Use `@RequestBody Mono<T>` for large payloads** to avoid blocking on request body parsing.
* [ ] **Configure proper codecs** for message reading/writing (JSON, XML, etc.).
* [ ] **Handle streaming responses** properly with `text/event-stream` or `application/stream+json`.

---

### 🗃️ **Data Access Layer**

* [ ] Use **R2DBC** instead of JDBC for relational DB access.
* [ ] For NoSQL, use **ReactiveMongoTemplate**, **ReactiveRedisTemplate**, etc.
* [ ] Avoid traditional Spring Data repositories (which are blocking).
* [ ] Ensure **database drivers** are non-blocking (e.g., PostgreSQL R2DBC).
* [ ] **Implement proper connection pooling** for R2DBC with `ConnectionFactoryOptions`.
* [ ] **Use `@Transactional` carefully** with reactive transactions (`ReactiveTransactionManager`).
* [ ] **Implement proper database health checks** using reactive health indicators.
* [ ] **Handle database connection timeouts** reactively.

---

### 🌐 **WebClient for External Calls**

* [ ] Always use `WebClient` (not `RestTemplate`) for HTTP calls.
* [ ] Use `.exchangeToMono()` or `.retrieve().bodyToMono()` to map responses.
* [ ] Handle timeouts using `.timeout(Duration)` operator.
* [ ] Use `.onStatus()` to gracefully handle non-2xx responses.
* [ ] **Configure connection pooling** and keep-alive settings for WebClient.
* [ ] **Implement retry logic** with exponential backoff using `.retryWhen()`.
* [ ] **Use proper codecs** for request/response serialization.
* [ ] **Handle streaming responses** from external services properly.

---

### 🧵 **Threading & Schedulers**

* [ ] Avoid using `.subscribeOn(Schedulers.boundedElastic())` unless absolutely required (e.g., file I/O).
* [ ] Keep all reactive chains on the **default Netty event loop** unless blocking task forces offloading.
* [ ] Never use `.block()`, `.subscribe()`, or `.toFuture().get()` inside the reactive pipeline. In controller/service, always return the Mono/Flux instead of subscribing. In WebFlux, let the framework handle subscription. Your job is to compose the pipeline. Use `.subscribe()` only needed, example invoking remote calls `webClient.get().uri("/data").retrieve().bodyToMono(String.class).subscribe(...)`.
* [ ] Use .subscribe() in consumer scenarios - Logging side effects, Background jobs, Making external calls, Application startup hooks etc.
* [ ] **Use `Schedulers.parallel()` for CPU-intensive tasks** that can be parallelized.
* [ ] **Understand scheduler lifecycle** and avoid scheduler leaks in tests.
* [ ] **Configure custom schedulers** appropriately for specific use cases.

---

### 📝 **Configuration & Properties**

* [ ] **Configure Netty server settings** (`server.netty.*`) for optimal performance.
* [ ] **Set appropriate buffer sizes** for request/response handling.
* [ ] **Configure connection timeouts** and keep-alive settings.
* [ ] **Tune memory settings** for optimal garbage collection with reactive workloads.
* [ ] **Configure proper logging levels** for reactive libraries.
* [ ] **Set up graceful shutdown** with appropriate timeout periods.

---

### 🚀 **Performance & Optimization**

* [ ] **Use `publishOn()` judiciously** – understand its impact on performance.
* [ ] **Implement proper caching strategies** with reactive-aware cache implementations.
* [ ] **Use `Flux.buffer()` and `Flux.window()`** for batch processing scenarios.
* [ ] **Optimize JSON serialization/deserialization** with appropriate Jackson configurations.
* [ ] **Monitor reactive stream performance** using custom metrics.
* [ ] **Implement proper pagination** for large result sets using `Flux.skip()` and `Flux.take()`.

---

### 🧪 **Testing & Debugging**

* [ ] Use **`StepVerifier`** for unit testing reactive code.
* [ ] Enable **reactor debug mode** during development:

  ```java
  Hooks.onOperatorDebug(); // Or use reactor-tools
  ```
* [ ] Log signal flow using `.log()` or `.doOnEach()` during debugging.
* [ ] Use tools like **BlockHound** to detect accidental blocking calls.
* [ ] **Write integration tests** using `@WebFluxTest` and `WebTestClient`.
* [ ] **Test error scenarios** thoroughly with reactive error handling.
* [ ] **Use `TestPublisher`** for testing complex reactive scenarios.
* [ ] **Implement proper test data cleanup** in reactive tests.

---

### 🛡️ **BlockHound Integration**

* [ ] **Add BlockHound dependency** to detect blocking calls in reactive contexts:
  ```xml
  <dependency>
      <groupId>io.projectreactor.tools</groupId>
      <artifactId>blockhound</artifactId>
      <version>1.0.8.RELEASE</version>
      <scope>runtime</scope>
  </dependency>
  ```
* [ ] **Configure environment-specific activation** - enable in `local`, `dev`, `qa` but disable in `prod` for performance:
  ```java
  @Configuration
  @Profile("!prod")  // Active for all profiles except production
  public class BlockHoundConfig { ... }
  ```
* [ ] **Use reflection-based setup** to avoid compile-time dependencies on BlockHound classes.
* [ ] **Configure logging-only mode** instead of throwing exceptions to avoid breaking development flow:
  ```java
  builderClass.getMethod("blockingMethodCallback", Consumer.class)
             .invoke(builder, loggingCallback);
  ```
* [ ] **Set up comprehensive allowlist** for safe framework operations:
  - UUID generation (`java.util.UUID.randomUUID`)
  - Jackson JSON operations
  - Spring Framework internals
  - Log4j2 operations
  - R2DBC reactive operations
* [ ] **Add monitoring tags** for Grafana/alerting integration:
  ```java
  log.warn("[BLOCKHOUND_VIOLATION] 🚫 REACTIVE VIOLATION: Blocking call detected: {}", method);
  log.warn("marker=BLOCKHOUND_VIOLATION severity=HIGH component=reactive-compliance method={}", method);
  ```
* [ ] **Configure Grafana dashboards** to monitor violations:
  ```promql
  # Count violations over time
  sum(rate(log_messages_total{message=~".*BLOCKHOUND_VIOLATION.*"}[5m]))
  
  # Top offending methods
  topk(10, count by (method)({job="service"} |= "marker=BLOCKHOUND_VIOLATION"))
  ```
* [ ] **Set up alerting rules** for high violation frequency or new blocking methods.
* [ ] **Create test cases** to verify BlockHound detects violations in non-production environments.
* [ ] **Document common violations** and their reactive alternatives for team education.
* [ ] **Integrate with CI/CD pipeline** to catch violations during integration testing.
* [ ] **Monitor false positives** and adjust allowlist as needed for third-party libraries.
* [ ] **Use BlockHound feedback** to identify and refactor blocking code patterns.

---

### 📊 **Monitoring & Observability**

* [ ] Integrate **Micrometer** with Prometheus/Grafana for metrics.
* [ ] Propagate **traceId** / **spanId** through MDC using `Context` and `ReactorContext`.
* [ ] Use **OpenTelemetry** or **Spring Observability** for end-to-end tracing.
* [ ] **Monitor reactive stream health** with custom health indicators.
* [ ] **Implement proper alerting** for reactive application metrics.
* [ ] **Use structured logging** with correlation IDs for reactive flows.

---

### 🔐 **Security & Resilience**

* [ ] Use `spring-boot-starter-oauth2-resource-server` for non-blocking JWT auth.
* [ ] Apply rate-limiting/reactive circuit breakers via **Resilience4j** or **Spring Cloud Gateway**.
* [ ] Handle errors reactively via `.onErrorResume()`, `.retry()`, or `.timeout()`.
* [ ] **Implement proper CORS configuration** for reactive endpoints.
* [ ] **Use reactive security context** properly with `ReactiveSecurityContextHolder`.
* [ ] **Implement request throttling** using reactive operators.
* [ ] **Handle security errors** reactively without blocking.

---

### 🧼 **Clean Code & Design**

* [ ] Keep controller → service → repository flow clear and layered with `Mono/Flux`.
* [ ] Use DTOs and records to decouple models from transport payloads.
* [ ] Apply **SOLID principles** and avoid anti-patterns like nested `flatMap` pyramids.
* [ ] Apply **Reactive Composition over Imperative Conditionals** (e.g., `.filter().switchIfEmpty()` instead of `if` blocks).
* [ ] **Implement proper exception handling** with custom error responses.
* [ ] **Use functional programming patterns** effectively in reactive chains.
* [ ] **Avoid reactive code duplication** by extracting common operators.

---

### 📦 **Project Setup**

* [ ] Use Spring Boot ≥ 3.2.x (aligned with Java 21 support).
* [ ] Add necessary reactive dependencies:

  ```xml
  <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-webflux</artifactId>
  </dependency>
  <dependency>
      <groupId>io.r2dbc</groupId>
      <artifactId>r2dbc-postgresql</artifactId>
  </dependency>
  ```
* [ ] **Configure proper dependency versions** to avoid compatibility issues.
* [ ] **Set up reactive-friendly IDE plugins** for better development experience.
* [ ] **Configure build tools** (Maven/Gradle) for optimal reactive application builds.

---

### 🧠 **Pro-Tips**

* [ ] Use `.cache()` in `Mono/Flux` to avoid re-subscribing to cold publishers for shared data.
* [ ] Understand cold vs hot publishers: avoid caching live data without expiration.
* [ ] Validate all service boundaries—**ensure reactive end-to-end**, no blocking creep from legacy code.
* [ ] Annotate blocking code paths during tech debt reviews for eventual reactive migration.
* [ ] **Use `Mono.defer()`** for lazy evaluation of expensive operations.
* [ ] **Implement proper resource cleanup** using `using()` and `usingWhen()` operators.
* [ ] **Understand subscription timing** and avoid premature subscription.
* [ ] **Use `Sinks`** for programmatic emission in complex scenarios.
* [ ] **Implement proper context propagation** for cross-cutting concerns.
* [ ] **Monitor memory usage** patterns specific to reactive applications.



## ❌ **DON'T Checklist - Common Anti-Patterns to Avoid**

### 🚫 **Blocking Operations**
* [ ] **DON'T** use `Thread.sleep()`, `Object.wait()`, or any blocking synchronization primitives
* [ ] **DON'T** use `InputStream`/`OutputStream` or any blocking I/O operations
* [ ] **DON'T** use traditional JDBC (`JdbcTemplate`, `EntityManager`) in reactive flows
* [ ] **DON'T** use `RestTemplate` for HTTP calls - always use `WebClient`
* [ ] **DON'T** use blocking collection operations like `Collections.synchronizedList()`
* [ ] **DON'T** use `CountDownLatch`, `Semaphore`, or other blocking concurrency utilities

### 🚫 **Reactive Stream Violations**
* [ ] **DON'T** call `.block()`, `.subscribe()`, or `.toFuture().get()` in reactive pipelines
* [ ] **DON'T** return raw types (`String`, `List<T>`) from controller methods - always wrap in `Mono`/`Flux`
* [ ] **DON'T** mutate shared state inside `.map()`, `.flatMap()`, or other operators
* [ ] **DON'T** use imperative loops (`for`, `while`) to process `Flux` elements
* [ ] **DON'T** catch exceptions with try-catch blocks in reactive chains
* [ ] **DON'T** use `.subscribe()` for fire-and-forget operations without proper error handling

### 🚫 **Threading Mistakes**
* [ ] **DON'T** create your own threads or use `Executors` unless absolutely necessary
* [ ] **DON'T** use `.subscribeOn(Schedulers.boundedElastic())` for CPU-bound tasks
* [ ] **DON'T** mix blocking and non-blocking code in the same reactive chain
* [ ] **DON'T** use `ThreadLocal` variables in reactive contexts
* [ ] **DON'T** assume thread affinity in reactive operators

### 🚫 **WebFlux Controller Mistakes**
* [ ] **DON'T** use `@ResponseBody` methods returning blocking types
* [ ] **DON'T** use `MultipartFile` for file uploads - use reactive `Part` instead
* [ ] **DON'T** access `HttpServletRequest` or `HttpServletResponse` directly
* [ ] **DON'T** use `@ModelAttribute` with blocking model binding
* [ ] **DON'T** return `ResponseEntity<T>` - use `Mono<ResponseEntity<T>>` instead

### 🚫 **Data Access Anti-Patterns**
* [ ] **DON'T** use `@Repository` with traditional Spring Data JPA repositories
* [ ] **DON'T** mix R2DBC and JDBC in the same transaction
* [ ] **DON'T** use blocking database connection pools
* [ ] **DON'T** perform N+1 queries without batching in reactive flows
* [ ] **DON'T** use lazy loading with JPA entities in reactive contexts

### 🚫 **Error Handling Mistakes**
* [ ] **DON'T** swallow errors with empty `.onErrorResume()` handlers
* [ ] **DON'T** use generic `Exception` catching without specific error types
* [ ] **DON'T** throw exceptions directly in reactive operators
* [ ] **DON'T** ignore backpressure errors or subscription failures
* [ ] **DON'T** use blocking logging frameworks without proper configuration

### 🚫 **Testing Anti-Patterns**
* [ ] **DON'T** use `.block()` in test assertions - use `StepVerifier` instead
* [ ] **DON'T** forget to verify complete/error signals in tests
* [ ] **DON'T** use blocking test utilities with reactive code
* [ ] **DON'T** ignore test subscription lifecycle management
* [ ] **DON'T** test reactive code without proper async context

### 🚫 **Performance Killers**
* [ ] **DON'T** cache `Flux` streams without understanding subscription semantics
* [ ] **DON'T** create unnecessary intermediate collections in reactive chains
* [ ] **DON'T** use `.collectList()` for large datasets without pagination
* [ ] **DON'T** ignore memory consumption in long-running reactive streams
* [ ] **DON'T** use synchronous operations in hot paths

### 🚫 **Security Violations**
* [ ] **DON'T** access security context using blocking `SecurityContextHolder`
* [ ] **DON'T** perform authentication/authorization checks using blocking methods
* [ ] **DON'T** store sensitive data in reactive context without proper cleanup
* [ ] **DON'T** ignore CSRF protection for state-changing operations
* [ ] **DON'T** expose internal error details in reactive error responses

### 🚫 **Configuration Mistakes**
* [ ] **DON'T** use default Netty configurations for production workloads
* [ ] **DON'T** ignore connection pool sizing and timeout configurations
* [ ] **DON'T** mix servlet and reactive stack dependencies
* [ ] **DON'T** use blocking health check implementations
* [ ] **DON'T** configure inappropriate buffer sizes for your use case

### 🚫 **BlockHound Mistakes**
* [ ] **DON'T** enable BlockHound in production environments - it impacts performance
* [ ] **DON'T** use default BlockHound behavior that throws exceptions - use logging-only mode
* [ ] **DON'T** ignore BlockHound violations - address them or add to allowlist if justified
* [ ] **DON'T** add everything to BlockHound allowlist - defeats the purpose of detection
* [ ] **DON'T** forget to configure monitoring for BlockHound violations
* [ ] **DON'T** use compile-time dependencies on BlockHound - use reflection instead
* [ ] **DON'T** assume all blocking calls are violations - some framework operations are safe




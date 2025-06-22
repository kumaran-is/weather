# ADR-0003: Adopt Java 21 Language Features

## Status
Accepted

## Context

Java 21 is the latest LTS (Long Term Support) release, providing significant language improvements and performance enhancements. Our current codebase was initially designed for Java 17, and we need to decide whether to upgrade and leverage the new language features.

### Key Java 21 Features Evaluated

1. **Pattern Matching for Switch**: Enhanced switch expressions with pattern matching
2. **Record Patterns**: Destructuring records in pattern matching contexts
3. **String Templates** (Preview): Enhanced string interpolation
4. **Sequenced Collections**: New collection interfaces with predictable ordering
5. **Virtual Threads**: Lightweight threads for improved concurrency
6. **Scoped Values** (Preview): Alternative to ThreadLocal for structured concurrency

### Current Pain Points

- **Complex conditional logic** in error handling and validation
- **Verbose exception mapping** in service layers
- **Collection ordering uncertainty** in result sets
- **Type-heavy switch statements** for enum and object processing

## Decision

We will adopt **Java 21** and leverage the following language features strategically:

### 1. Pattern Matching for Switch Expressions

Replace traditional if-else chains and verbose switch statements with modern pattern matching.

```java
// Before: Traditional if-else
private ErrorResponse buildErrorResponse(Throwable ex) {
    if (ex instanceof BaseException baseEx) {
        return new ErrorResponse(baseEx.getCode(), ...);
    } else if (ex instanceof WebExchangeBindException bindEx) {
        return new ErrorResponse("VALIDATION_ERROR", ...);
    }
    return new ErrorResponse("INTERNAL_SERVER_ERROR", ...);
}

// After: Pattern matching switch
private ErrorResponse buildErrorResponse(Throwable ex) {
    return switch (ex) {
        case BaseException baseEx -> new ErrorResponse(baseEx.getCode(), ...);
        case WebExchangeBindException bindEx -> {
            var errors = buildValidationErrors(bindEx);
            yield new ErrorResponse("VALIDATION_ERROR", ..., errors);
        }
        case TimeoutException tEx -> new ErrorResponse("TIMEOUT", ...);
        default -> new ErrorResponse("INTERNAL_SERVER_ERROR", ...);
    };
}
```

### 2. Sequenced Collections

Use new collection interfaces for predictable ordering in results.

```java
// Enhanced collection handling with guaranteed ordering
private SequencedCollection<String> buildOrderedUniqueCollection(List<String> cities) {
    SequencedCollection<String> orderedCities = new LinkedHashSet<>();
    orderedCities.addAll(cities);
    return orderedCities;
}
```

### 3. Record Patterns (Future Enhancement)

Prepare for record pattern matching when it becomes stable.

### 4. Virtual Threads (Isolation Boundary)

Keep virtual threads separate from reactive stacks, use only for specific blocking operations if necessary.

## Consequences

### Positive

- **Improved Code Readability**: Pattern matching eliminates verbose conditional logic
- **Enhanced Type Safety**: Compile-time verification of pattern completeness
- **Better Performance**: JVM optimizations for modern language constructs
- **Future-Proof Codebase**: Leveraging latest language evolution
- **Reduced Boilerplate**: More concise and expressive code
- **Predictable Collections**: Guaranteed ordering with SequencedCollection
- **Maintainability**: Cleaner error handling and validation logic

### Negative

- **Team Learning Curve**: Developers need to learn new language features
- **IDE Support Variance**: Some IDEs may have limited support for newest features
- **Debugging Changes**: New language constructs may require different debugging approaches
- **Library Compatibility**: Some older libraries may not leverage new features
- **Migration Effort**: Existing code needs gradual migration

### Neutral

- **Build System Updates**: Maven/Gradle configuration changes required
- **Deployment Considerations**: Runtime environment must support Java 21
- **Tool Chain Updates**: CI/CD and development tools need updates

## Compliance

### Development Standards

1. **Pattern Matching Usage**:
   - Use pattern matching switch for exception handling
   - Replace complex if-else chains with pattern matching
   - Ensure exhaustive pattern coverage

2. **Collection Handling**:
   - Use `SequencedCollection` for ordered results
   - Prefer `LinkedHashSet` for unique ordered collections
   - Document ordering guarantees in method contracts

3. **Code Review Guidelines**:
   - Verify pattern matching completeness
   - Check for proper use of sequenced collections
   - Ensure no mixing of virtual threads with reactive streams

### Implementation Examples

#### Exception Mapping with Pattern Matching
```java
private Throwable mapToServiceException(Throwable throwable) {
    return switch (throwable) {
        case WeatherNotFoundException wnfEx -> {
            log.debug("Weather data not found: {}", wnfEx.getMessage());
            yield wnfEx;
        }
        case WeatherValidationException wvEx -> {
            log.debug("Validation error: {}", wvEx.getMessage());
            yield wvEx;
        }
        case DataAccessException daEx -> {
            log.error("Database access error", daEx);
            yield new WeatherServiceException("Database operation failed", daEx);
        }
        default -> {
            log.error("Unexpected error", throwable);
            yield new WeatherServiceException("Processing failed", throwable);
        }
    };
}
```

#### Validation with Switch Expressions
```java
private Mono<Void> validateWeatherCondition(String condition) {
    if (condition == null || condition.isBlank()) {
        return Mono.empty();
    }
    
    return switch (condition.toLowerCase().trim()) {
        case "clear", "sunny", "cloudy", "overcast", "rainy", 
             "stormy", "snowy", "foggy", "windy" -> Mono.empty();
        default -> Mono.error(new WeatherValidationException(
            "Invalid weather condition: " + condition));
    };
}
```

### Build Configuration

```xml
<!-- pom.xml -->
<properties>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
</properties>

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <source>21</source>
        <target>21</target>
        <compilerArgs>
            <arg>--enable-preview</arg> <!-- Only if using preview features -->
        </compilerArgs>
    </configuration>
</plugin>
```

### Migration Strategy

1. **Phase 1**: Update build configuration and basic language features
2. **Phase 2**: Migrate exception handling to pattern matching
3. **Phase 3**: Implement sequenced collections for ordered results
4. **Phase 4**: Explore advanced features (records patterns, string templates)

### Excluded Features

- **Virtual Threads**: Not used in reactive context to avoid mixing paradigms
- **String Templates**: Keeping as preview, evaluate in future releases
- **Scoped Values**: Not needed in current stateless reactive architecture

## Notes

### Performance Impact

- **Compilation Time**: Slight increase due to pattern matching analysis
- **Runtime Performance**: Improved due to JVM optimizations
- **Memory Usage**: Minimal impact, potential improvements with new GC features

### Team Training Requirements

1. **Pattern Matching Workshop**: 2-hour session on switch expressions
2. **Sequenced Collections**: Understanding ordering guarantees
3. **Migration Guidelines**: Best practices for converting existing code
4. **Code Review Training**: Recognizing and reviewing new language features

### Future Considerations

- **Record Patterns**: Evaluate when stable (likely Java 22+)
- **String Templates**: Consider adoption when finalized
- **Structured Concurrency**: Monitor development for potential reactive integration

---

**Last Updated:** 2024-01-15  
**Authors:** Weather Service Team  
**Reviewers:** Java Platform Team, Architecture Review Board
# ADR-0005: RESTful API Design with OpenAPI Specification

## Status
Accepted

## Context

The weather service needs a well-designed, documented, and maintainable API that follows industry standards and best practices. The API will be consumed by web applications, mobile apps, and potentially third-party integrations.

### Requirements

1. **RESTful Design**: Follow REST architectural principles
2. **Comprehensive Documentation**: Auto-generated, interactive API documentation
3. **Validation**: Strong input validation and error handling
4. **Versioning**: Future-proof API versioning strategy
5. **Security**: Authentication and authorization capabilities
6. **Reactive Compatibility**: Align with reactive architecture (ADR-0001)
7. **Developer Experience**: Easy to understand and integrate

### Evaluated Approaches

1. **GraphQL**: Query-based API with flexible data fetching
2. **REST with OpenAPI**: Traditional REST with comprehensive documentation
3. **gRPC**: High-performance RPC for microservices
4. **Custom JSON API**: Proprietary API design
5. **HAL/JSON-API**: Hypermedia-driven REST APIs

## Decision

We will implement **RESTful API design with OpenAPI 3.0 specification** using the following principles:

### Core Design Principles

1. **Resource-Oriented URLs**: Clear, hierarchical resource structure
2. **HTTP Methods**: Proper use of GET, POST, PUT, DELETE
3. **Status Codes**: Meaningful HTTP status codes
4. **Content Negotiation**: JSON as primary format
5. **Pagination**: Consistent pagination for collections
6. **Error Handling**: Structured error responses
7. **Validation**: Comprehensive input validation

### API Structure

```
/api/v1/weather                          # Weather data collection
├── POST                                 # Create weather data
├── GET /api/v1/weather/{id}            # Get by ID
├── PUT /api/v1/weather/{id}            # Update weather data
├── DELETE /api/v1/weather/{id}         # Delete weather data
├── GET /api/v1/weather/city/{city}     # Get by city (paginated)
├── GET /api/v1/weather/city/{city}/latest  # Latest data for city
├── GET /api/v1/weather/range           # Get by date range
├── GET /api/v1/weather/city/{city}/range   # Get by city and date range
└── GET /api/v1/weather/cities          # Get all cities
```

## Consequences

### Positive

- **Industry Standard**: Widely understood RESTful patterns
- **Excellent Tooling**: Rich ecosystem of REST tools and libraries
- **Auto-Documentation**: OpenAPI generates interactive documentation
- **Client Generation**: Auto-generate client SDKs from OpenAPI spec
- **Validation Framework**: Built-in validation with Spring Boot
- **Caching Support**: HTTP caching mechanisms available
- **Reactive Compatible**: Works seamlessly with Spring WebFlux
- **Developer Friendly**: Familiar patterns for most developers

### Negative

- **Over-fetching**: Clients may receive more data than needed
- **Multiple Requests**: Complex queries require multiple API calls
- **Rigid Structure**: Less flexible than GraphQL for varied data needs
- **Documentation Maintenance**: OpenAPI annotations require maintenance

### Neutral

- **Performance**: Good performance, but not as optimal as gRPC
- **Learning Curve**: Minimal for developers familiar with REST
- **Ecosystem Size**: Large ecosystem with many options

## Compliance

### URL Design Standards

```java
// ✅ Good: Resource-oriented, hierarchical
GET /api/v1/weather/city/London?page=0&size=20
GET /api/v1/weather/city/London/latest
GET /api/v1/weather/range?start=2024-01-01T00:00:00&end=2024-01-31T23:59:59

// ❌ Bad: Action-oriented, non-hierarchical
GET /api/v1/getWeatherByCity?city=London
POST /api/v1/weather/search
```

### HTTP Status Code Usage

| Status Code | Usage | Example |
|-------------|-------|---------|
| 200 OK | Successful GET, PUT | Data retrieved/updated |
| 201 Created | Successful POST | Weather data created |
| 204 No Content | Successful DELETE | Data deleted |
| 400 Bad Request | Validation errors | Invalid input data |
| 404 Not Found | Resource not found | Weather data ID not found |
| 409 Conflict | Business rule violation | Duplicate weather entry |
| 429 Too Many Requests | Rate limiting | API rate limit exceeded |
| 500 Internal Server Error | Server errors | Database connection failure |
| 503 Service Unavailable | Circuit breaker open | Database temporarily unavailable |

### Request/Response Standards

#### Successful Response Format
```json
// Single Resource
{
    "id": 1,
    "city": "London",
    "country": "UK",
    "temperature": 15.5,
    "humidity": 75,
    "pressure": 1013.25,
    "windSpeed": 10.0,
    "windDirection": "W",
    "weatherCondition": "Cloudy",
    "description": "Overcast with light winds",
    "recordedAt": "2024-01-15T10:00:00",
    "createdAt": "2024-01-15T10:15:00",
    "updatedAt": "2024-01-15T10:15:00"
}

// Paginated Collection
{
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "first": true,
    "last": false,
    "numberOfElements": 20,
    "empty": false
}
```

#### Error Response Format
```json
{
    "code": "WEATHER_NOT_FOUND",
    "message": "Weather data not found with id: 999",
    "status": 404,
    "path": "/api/v1/weather/999",
    "timestamp": "2024-01-15T10:30:00",
    "validationErrors": null
}

// Validation Error Response
{
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "status": 400,
    "path": "/api/v1/weather",
    "timestamp": "2024-01-15T10:30:00",
    "validationErrors": [
        {
            "field": "city",
            "rejectedValue": "",
            "message": "City is required"
        }
    ]
}
```

### OpenAPI Documentation Standards

```java
@Operation(
    summary = "Create weather data",
    description = "Create a new weather data record with comprehensive validation",
    responses = {
        @ApiResponse(
            responseCode = "201",
            description = "Weather data created successfully",
            content = @Content(
                schema = @Schema(implementation = WeatherDataResponse.class),
                examples = @ExampleObject(
                    name = "Successful Creation",
                    value = """
                    {
                        "id": 1,
                        "city": "Paris",
                        "temperature": 18.5,
                        ...
                    }
                    """
                )
            )
        )
    }
)
```

### Validation Rules

1. **Input Validation**: All request bodies validated with Bean Validation
2. **Path Parameters**: Type validation and range checking
3. **Query Parameters**: Default values and constraint validation
4. **Business Rules**: Custom validation in service layer

```java
public record WeatherDataRequest(
    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City name must not exceed 100 characters")
    String city,
    
    @NotNull(message = "Temperature is required")
    @DecimalMin(value = "-100.00", message = "Temperature must be greater than -100°C")
    @DecimalMax(value = "100.00", message = "Temperature must be less than 100°C")
    BigDecimal temperature,
    
    @NotNull(message = "Recorded time is required")
    LocalDateTime recordedAt
) {}
```

### Pagination Standards

- **Zero-based indexing**: `page=0` for first page
- **Default page size**: 20 items per page
- **Maximum page size**: 100 items per page
- **Sort parameters**: Support for field-based sorting
- **Consistent response format**: Always include pagination metadata

### API Versioning Strategy

1. **URL Versioning**: `/api/v1/` prefix for version 1
2. **Backward Compatibility**: Maintain previous versions during transition
3. **Deprecation Policy**: 6-month notice before version removal
4. **Version Documentation**: Clear migration guides between versions

### Security Considerations

```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/v1/weather/**").authenticated()
                        .pathMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt())
                .build();
    }
}
```

## Notes

### OpenAPI Configuration

```java
@OpenAPIDefinition(
    info = @Info(
        title = "Weather Service API",
        version = "1.0.0",
        description = "A reactive REST service for managing weather data",
        contact = @Contact(name = "Weather Service Team", email = "weather-service@example.com"),
        license = @License(name = "MIT License", url = "https://opensource.org/licenses/MIT")
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Local server"),
        @Server(url = "https://api.weather-service.com", description = "Production server")
    }
)
```

### Content Type Standards

- **Request Content-Type**: `application/json`
- **Response Content-Type**: `application/json`
- **Character Encoding**: UTF-8
- **Date Format**: ISO 8601 (yyyy-MM-dd'T'HH:mm:ss)

### Rate Limiting Strategy

```yaml
resilience4j:
  ratelimiter:
    instances:
      weather-api:
        limit-for-period: 100
        limit-refresh-period: 1s
        timeout-duration: 1s
```

### API Testing Strategy

1. **Contract Testing**: OpenAPI specification as contract
2. **Integration Testing**: Full request/response cycle testing
3. **Load Testing**: Performance testing with realistic payloads
4. **Security Testing**: Authentication and authorization testing

### Client SDK Generation

OpenAPI specification enables automatic generation of client SDKs:
- **JavaScript/TypeScript**: For web applications
- **Java**: For Spring Boot microservices
- **Python**: For data analysis tools
- **Mobile SDKs**: iOS (Swift) and Android (Kotlin)

---

**Last Updated:** 2024-01-15  
**Authors:** Weather Service Team  
**Reviewers:** API Platform Team, Frontend Team
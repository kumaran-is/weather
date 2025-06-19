# Weather Service

A reactive REST service for managing weather data, built with Java 21, Spring Boot 3.4.5, and Spring WebFlux.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Building the Project](#building-the-project)
- [Running the Application](#running-the-application)
- [Health Checks & Monitoring](#health-checks--monitoring)
- [Database Access via H2 Console](#database-access-via-h2-console)
- [API Testing with Swagger UI](#api-testing-with-swagger-ui)
- [API Endpoints Reference](#api-endpoints-reference)
- [Development Workflow](#development-workflow)
- [Configuration](#configuration)
- [Docker Support](#docker-support)
- [Logging](#logging)
- [Error Handling](#error-handling)
- [Resilience Patterns](#resilience-patterns)

## Overview

This service provides RESTful endpoints to record, retrieve, update, and delete weather information for various cities. It uses a reactive stack for non-blocking I/O and is designed to be scalable and resilient with comprehensive monitoring and health checks.

## Features

- **Reactive CRUD operations** for weather data using Spring WebFlux
- **H2 in-memory database** for local development with sample data
- **SQL Server support** for dev/prod environments via R2DBC
- **Java Records** for DTOs (no Lombok dependency for DTOs)
- **Comprehensive health checks** (application, database, resilience components)
- **Resilience patterns** (Circuit Breaker, Retry, Rate Limiter, Time Limiter)
- **Global error handling** with standardized JSON error responses
- **OpenAPI/Swagger documentation** with example request values
- **Validation** using JSR-380 annotations
- **Paginated responses** for large datasets
- **Docker support** with multi-stage builds and security best practices
- **Single port configuration** (8080) for all services

## Technology Stack

- **Java 21** with Records and modern features
- **Spring Boot 3.4.5** with Spring Framework 6.2.6
- **Spring WebFlux** for reactive web programming
- **Spring Data R2DBC** for reactive database access
- **Spring Security** with permissive configuration for API access
- **Spring Boot Actuator** for health checks and metrics
- **Project Reactor** for reactive programming
- **Maven 3.9.6** for build management
- **R2DBC** drivers for H2 and SQL Server
- **H2 Database** for local development
- **Log4j2** for structured logging
- **Resilience4j** for resilience patterns
- **Springdoc OpenAPI** for API documentation
- **MapStruct** for entity-DTO mapping
- **Docker** with Eclipse Temurin Alpine images

## Prerequisites

- **JDK 21** or later
- **Maven 3.8.0** or later
- **Docker** (optional, for containerization)
- **IDE** with Java support (IntelliJ IDEA, VS Code, Eclipse)

## Quick Start

1. **Clone and build the project:**
   ```bash
   git clone <repository-url>
   cd weather
   ./mvnw clean package
   ```

2. **Run the application:**
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

3. **Access the application:**
   - **Swagger UI**: http://localhost:8080/swagger-ui.html
   - **Health Check**: http://localhost:8080/management/health
   - **API Base**: http://localhost:8080/api/v1/weather
   - **H2 Console**: http://localhost:8080/h2-console

## Project Structure

```
weather/
├── src/main/java/com/weather/
│   ├── controller/          # REST controllers
│   ├── service/            # Business logic layer
│   │   └── impl/           # Service implementations
│   ├── repository/         # R2DBC repositories
│   ├── entity/             # JPA entities (with Lombok)
│   ├── dto/                # Data Transfer Objects (Java Records)
│   ├── mapper/             # MapStruct mappers
│   ├── config/             # Configuration classes
│   ├── exception/          # Custom exceptions & global error handler
│   ├── health/             # Custom health indicators
│   └── WeatherServiceApplication.java
├── src/main/resources/
│   ├── application.yml     # Base configuration
│   ├── application-local.yml  # H2 local configuration
│   ├── application-dev.yml    # SQL Server dev configuration
│   ├── log4j2.xml         # Logging configuration
│   ├── schema.sql         # H2 database schema
│   ├── schema-mssql.sql   # SQL Server schema
│   └── data.sql           # Sample data for H2
├── src/test/              # Unit and integration tests
├── Dockerfile             # Multi-stage Docker build
├── docker-compose.yml     # Docker Compose configuration
└── README.md
```

## Building the Project

### Build with tests:
```bash
./mvnw clean package
```

### Build without tests:
```bash
./mvnw clean package -DskipTests
```

### Clean and install dependencies:
```bash
./mvnw clean install
```

## Running the Application

### Option 1: Using Maven (Recommended for Development)
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Option 2: Using JAR file
```bash
# Build first
./mvnw clean package

# Run with local profile (H2 database)
java -jar target/weather-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

### Option 3: Using Docker
```bash
# Build and run with Docker Compose
docker-compose up weather-service

# Or build and run manually
docker build -t weather-service .
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=local weather-service
```

**Application will start on:** http://localhost:8080

## Health Checks & Monitoring

Before testing the API, verify that all components are healthy:

### 1. Application Health Check
```bash
curl http://localhost:8080/management/health
```
**Expected Response:**
```json
{
  "status": "UP",
  "groups": ["liveness", "readiness"]
}
```

### 2. Detailed Health Information
```bash
curl "http://localhost:8080/management/health?show-details=always"
```

### 3. Deep Health Check (Custom Endpoint)
```bash
curl http://localhost:8080/management/deephealth
```
**Monitors:**
- Database connectivity and response time
- Circuit breaker states
- Rate limiter status
- Retry metrics
- Time limiter performance

### 4. Application Info
```bash
curl http://localhost:8080/management/info
```

### 5. Resilience Metrics
Check individual resilience components:
```bash
# Circuit breaker metrics
curl http://localhost:8080/management/metrics/resilience4j.circuitbreaker.calls

# Retry metrics  
curl http://localhost:8080/management/metrics/resilience4j.retry.calls

# Rate limiter metrics
curl http://localhost:8080/management/metrics/resilience4j.ratelimiter.calls
```

**✅ All health checks should return `UP` status before proceeding to API testing.**

## Database Access via H2 Console

When running with the `local` profile, you can access the H2 in-memory database through a web console to inspect and query the data directly.

### Accessing H2 Console
1. **Open your browser** and navigate to: **http://localhost:8080/h2-console**

2. **Login with these credentials:**
   - **JDBC URL**: `jdbc:h2:mem:testdb` 
   - **Username**: `sa`
   - **Password**: (leave empty - no password)
   - **Driver Class**: `org.h2.Driver` (auto-filled)

3. **Click "Test Connection"** first to verify, then **"Connect"** to access the database

### Troubleshooting H2 Console Access
If you're having issues connecting:

1. **Ensure the application is running** with the `local` profile:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

2. **Try these alternative JDBC URLs** if the default doesn't work:
   - `jdbc:h2:mem:testdb` (simple format)
   - `jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE` (with parameters)
   - Check the application logs for the actual H2 URL being used

3. **Verify H2 console is enabled** by checking the logs for:
   ```
   H2 console available at '/h2-console'
   ```

4. **Make sure you're using the correct port**: `8080` (default)

### Available Tables
- **`weather_data`**: Main table containing all weather records
- **`city_lookup`**: Reference table for city information (if applicable)

### Sample Queries
```sql
-- View all weather data
SELECT * FROM weather_data ORDER BY recorded_at DESC;

-- Get weather data for a specific city
SELECT * FROM weather_data WHERE city = 'New York' ORDER BY recorded_at DESC;

-- Get latest weather data for each city
SELECT city, MAX(recorded_at) as latest_record 
FROM weather_data 
GROUP BY city;

-- Count total records
SELECT COUNT(*) as total_records FROM weather_data;

-- Average temperature by city
SELECT city, AVG(temperature) as avg_temp 
FROM weather_data 
GROUP BY city 
ORDER BY avg_temp DESC;
```

### Notes
- ⚠️ **H2 Console is only available in local profile** for security reasons
- 🔄 **Data is reset on application restart** (in-memory database)
- 📊 **Sample data is automatically loaded** from `data.sql` on startup
- 🔍 **Use this for debugging and data verification** during development

## API Testing with Swagger UI

### Access Swagger UI
Open your browser and navigate to: **http://localhost:8080/swagger-ui.html**

### Pre-populated Sample Data
The H2 database comes with sample weather data for these cities:
- New York, USA
- London, UK  
- Tokyo, Japan
- Sydney, Australia
- Mumbai, India
- Berlin, Germany
- Toronto, Canada
- Paris, France

### Testing Workflow with Swagger UI

#### 1. **GET All Cities**
- Endpoint: `GET /api/v1/weather/cities`
- Click "Try it out" → "Execute"
- **Expected**: List of available cities

#### 2. **GET Latest Weather for a City**
- Endpoint: `GET /api/v1/weather/city/{city}/latest`
- Enter city: `New York`
- **Expected**: Latest weather data for New York

#### 3. **GET Weather by City (Paginated)**
- Endpoint: `GET /api/v1/weather/city/{city}`
- Enter city: `London`
- page: `0`, size: `5`
- **Expected**: Paginated weather data for London

#### 4. **GET Weather by ID**
- Endpoint: `GET /api/v1/weather/{id}`
- Enter id: `1`
- **Expected**: Weather data with ID 1

#### 5. **POST Create New Weather Data**
- Endpoint: `POST /api/v1/weather`
- Use the pre-filled example or modify:
```json
{
  "city": "San Francisco",
  "country": "USA",
  "temperature": 18.5,
  "humidity": 72,
  "pressure": 1015.30,
  "windSpeed": 6.2,
  "windDirection": "W",
  "weatherCondition": "Foggy",
  "description": "Morning fog with cool breeze",
  "recordedAt": "2024-01-15T08:00:00"
}
```

#### 6. **GET Weather by Date Range**
- Endpoint: `GET /api/v1/weather/range`
- start: `2024-01-15T00:00:00`
- end: `2024-01-15T23:59:59`
- **Expected**: All weather data for January 15, 2024

#### 7. **PUT Update Weather Data**
- First create or get an existing ID
- Endpoint: `PUT /api/v1/weather/{id}`
- Modify the weather data and submit

#### 8. **DELETE Weather Data**
- Endpoint: `DELETE /api/v1/weather/{id}`
- Enter an existing ID
- **Expected**: 204 No Content response

### Default Example Values in Swagger

The Swagger UI comes with pre-configured example values:

**WeatherDataRequest Example:**
```json
{
  "city": "New York",
  "country": "USA", 
  "temperature": 22.5,
  "humidity": 65,
  "pressure": 1013.25,
  "windSpeed": 5.2,
  "windDirection": "NW",
  "weatherCondition": "Clear",
  "description": "Clear skies with light winds",
  "recordedAt": "2024-01-15T10:00:00"
}
```

These examples are defined in the DTO annotations and will work seamlessly with the H2 database.

## API Endpoints Reference

### Core Weather Endpoints
| Method | Endpoint | Description | Parameters |
|--------|----------|-------------|------------|
| POST | `/api/v1/weather` | Create weather data | Request body |
| GET | `/api/v1/weather/{id}` | Get weather by ID | `id` (path) |
| GET | `/api/v1/weather/city/{city}` | Get weather by city | `city` (path), `page`, `size` |
| GET | `/api/v1/weather/city/{city}/latest` | Get latest weather for city | `city` (path) |
| GET | `/api/v1/weather/range` | Get weather by date range | `start`, `end`, `page`, `size` |
| GET | `/api/v1/weather/city/{city}/range` | Get weather by city and date range | `city`, `start`, `end`, `page`, `size` |
| PUT | `/api/v1/weather/{id}` | Update weather data | `id` (path), Request body |
| DELETE | `/api/v1/weather/{id}` | Delete weather data | `id` (path) |
| GET | `/api/v1/weather/cities` | Get all cities | None |

### Management Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/management/health` | Application health |
| GET | `/management/deephealth` | Detailed health check |
| GET | `/management/info` | Application information |
| GET | `/management/metrics` | Application metrics |

### Documentation Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/swagger-ui.html` | Swagger UI |
| GET | `/v3/api-docs` | OpenAPI specification |
| GET | `/h2-console` | H2 Database Console (local profile only) |

## Development Workflow

1. **Make code changes**
2. **Run tests**: `./mvnw test`
3. **Build application**: `./mvnw clean package`
4. **Start with local profile**: `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
5. **Verify health**: Check all health endpoints
6. **Test with Swagger UI**: http://localhost:8080/swagger-ui.html
7. **Check logs**: `./logs/weather-service-local.log`

## Configuration

### Profiles
- **local**: H2 in-memory database, file logging, debug mode
- **dev**: SQL Server database, console logging, debug mode

### Key Configuration Files
- `application.yml`: Base configuration, resilience settings
- `application-local.yml`: H2 database, local logging
- `application-dev.yml`: SQL Server configuration
- `log4j2.xml`: Logging levels and appenders

### Environment Variables (Dev Profile)
```bash
export DB_URL_DEV=r2dbc:mssql://localhost:1433/weatherdb
export DB_USERNAME_DEV=weather_user
export DB_PASSWORD_DEV=your_password
```

## Docker Support

### Single Service
```bash
docker-compose up weather-service
```

### Development Environment with SQL Server
```bash
docker-compose --profile dev up
```

### Manual Docker Build
```bash
docker build -t weather-service .
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=local weather-service
```

## Logging

### Log Levels
- **Local Profile**: DEBUG level for `com.weather`, detailed R2DBC logging
- **Other Profiles**: INFO level for frameworks, DEBUG for application

### Log Files
- **Local**: `./logs/weather-service-local.log`
- **Console**: Real-time colored output

### Key Log Categories
- `com.weather`: Application logs
- `org.springframework.r2dbc`: Database operations
- `io.github.resilience4j`: Resilience component events

## Error Handling

### Global Exception Handler
- Catches all exceptions and returns standardized JSON responses
- Handles validation errors with field-level details
- Provides correlation with timestamp and request path

### Error Response Format
```json
{
  "code": "WEATHER_NOT_FOUND",
  "message": "Weather data not found with id: 123",
  "status": 404,
  "path": "/api/v1/weather/123",
  "timestamp": "2024-01-15T10:30:00",
  "validationErrors": null
}
```

## Resilience Patterns

### Circuit Breaker
- **Sliding window**: 10 calls
- **Failure threshold**: 50%
- **Wait duration**: 10 seconds
- **Half-open calls**: 3

### Retry
- **Max attempts**: 3
- **Wait duration**: 1 second
- **Retry on**: DataAccessException, SQLException

### Rate Limiter
- **Requests per second**: 100
- **Timeout**: 1 second

### Time Limiter
- **Timeout**: 3 seconds
- **Cancel running futures**: true

All resilience metrics are available via health checks and monitoring endpoints.

---

**🎉 Your Weather Service is ready! Start with the health checks, then explore the API using Swagger UI.**

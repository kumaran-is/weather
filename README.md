# Weather Service

A reactive REST service for managing weather data, built with Java 21, Spring Boot 3.4.5, and Spring WebFlux.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Project Setup](#project-setup)
  - [Configuration](#configuration)
  - [Building the Project](#building-the-project)
  - [Running the Application](#running-the-application)
- [API Endpoints](#api-endpoints)
  - [Health Checks](#health-checks)
  - [Swagger API Documentation](#swagger-api-documentation)
- [Development Workflow](#development-workflow)
- [Logging](#logging)
- [Error Handling](#error-handling)
- [Resilience](#resilience)
- [Dockerization](#dockerization)

## Overview

This service provides RESTful endpoints to record, retrieve, update, and delete weather information for various cities. It uses a reactive stack for non-blocking I/O and is designed to be scalable and resilient.

## Features

-   CRUD operations for weather data.
-   Reactive API endpoints.
-   H2 in-memory database for local development.
-   Support for other R2DBC-compatible databases for different environments (e.g., Azure SQL Server).
-   Centralized configuration management.
-   Comprehensive health checks (application, database, resilience components).
-   Global error handling with standardized error responses.
-   Resilience patterns (Circuit Breaker, Retry, Rate Limiter, Time Limiter) via Resilience4j.
-   OpenAPI (Swagger) documentation.
-   Docker support for containerization.

## Technology Stack

-   Java 21
-   Spring Boot 3.4.5
-   Spring Framework 6.2.6
    -   Spring WebFlux (Reactive Web)
    -   Spring Data R2DBC (Reactive Database Access)
    -   Spring Security
    -   Spring Boot Actuator
-   Project Reactor
-   Maven 3.8+
-   R2DBC (for H2, SQL Server, etc.)
-   H2 Database (for local profile)
-   Log4j2 (Logging)
-   Resilience4j (Resilience patterns)
-   Springdoc OpenAPI (Swagger API documentation)
-   Lombok (Boilerplate code reduction)
-   MapStruct (Bean mapping)
-   Docker

## Prerequisites

-   JDK 21 or later
-   Maven 3.8.0 or later
-   Docker (optional, for containerization)
-   An IDE that supports Java and Maven (e.g., IntelliJ IDEA, VS Code with Java extensions)

## Project Setup

1.  **Clone the repository** (if applicable) or ensure all files are in the project directory.
2.  **Import the project** into your IDE as a Maven project.
3.  **Resolve Maven dependencies**: The IDE should do this automatically, or you can run `mvn clean install` from the command line.

### Configuration

The application uses YAML-based configuration files located in `src/main/resources/`:

-   `application.yml`: Base configuration, common to all profiles.
-   `application-local.yml`: Configuration for the `local` profile (default for quick local runs). Uses H2 in-memory database and file-based logging.
-   `application-dev.yml`: Placeholder configuration for a `dev` profile, intended for a provisioned development database (e.g., Azure SQL). Uses console logging.
    -   To use this profile, you'll need to set environment variables like `DB_URL_DEV`, `DB_USERNAME_DEV`, `DB_PASSWORD_DEV`.
-   Other profiles (`qa`, `preprod`, `prod`) can be added similarly.

**Log4j2 Configuration**:
Logging is configured via `src/main/resources/log4j2.xml`.
-   For the `local` profile (when `logging.file.name` is set in `application-local.yml`), logs will be written to `./logs/weather-service-local.log`.
-   For other profiles, logs are typically directed to the console.

### Building the Project

To build the project and package it into a JAR file, run the following Maven command from the project root directory:

```bash
mvn clean package
```

This command will also run unit tests. To skip tests during the build:

```bash
mvn clean package -DskipTests
```

### Running the Application

You can run the application using Maven or by executing the JAR file directly.

**Using Maven:**

-   **Local Profile (H2 database):**
    ```bash
    mvn spring-boot:run -Dspring-boot.run.profiles=local
    ```
    (If no profile is specified, Spring Boot might not pick one up by default unless `spring.profiles.active` is set in `application.yml`, which is not the case here to enforce explicit profile activation).

-   **Dev Profile (requires environment variables for DB connection):**
    Ensure `DB_URL_DEV`, `DB_USERNAME_DEV`, `DB_PASSWORD_DEV` are set in your environment.
    ```bash
    mvn spring-boot:run -Dspring-boot.run.profiles=dev
    ```

**Using the JAR file:**

After building the project with `mvn clean package`, the JAR file will be located in the `target/` directory (e.g., `weather-service-0.0.1-SNAPSHOT.jar`).

-   **Local Profile:**
    ```bash
    java -jar target/weather-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
    ```

-   **Dev Profile:**
    ```bash
    java -jar target/weather-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
    ```
    (Ensure environment variables for DB connection are set).

The application typically starts on port `8080` (configurable via `SERVER_PORT` or in `application-<profile>.yml`).
Management endpoints are on port `8081` (configurable via `MGMT_PORT`).

## API Endpoints

The main API endpoints are rooted at `/api/v1/weather`.

-   `POST /api/v1/weather`: Add a new weather record.
-   `GET /api/v1/weather/{id}`: Get a weather record by its ID.
-   `GET /api/v1/weather/city/{city}`: Get weather records for a specific city (paginated).
-   `GET /api/v1/weather/city/{city}/latest`: Get the latest weather record for a city.
-   `GET /api/v1/weather/range?start=<datetime>&end=<datetime>`: Get weather records within a date range (paginated).
-   `GET /api/v1/weather/city/{city}/range?start=<datetime>&end=<datetime>`: Get weather records for a city within a date range (paginated).
-   `PUT /api/v1/weather/{id}`: Update an existing weather record.
-   `DELETE /api/v1/weather/{id}`: Delete a weather record.

### Health Checks

Spring Boot Actuator health endpoints are available under `/management` (default port `8081`):

-   **Standard Health**: `http://localhost:8081/management/health`
    -   Shows aggregated status. Add `?show-details=always` or configure `management.endpoint.health.show-details=always` for more details.
-   **Deep Health (Custom Aggregation)**: `http://localhost:8081/management/deephealth`
    -   Provides a detailed view of all custom `ReactiveHealthIndicator` beans.
-   **Liveness Probe**: `http://localhost:8081/management/health/liveness`
-   **Readiness Probe**: `http://localhost:8081/management/health/readiness`

### Swagger API Documentation

Once the application is running, API documentation is available via Swagger UI:

-   `http://localhost:8080/swagger-ui.html` (assuming server port is 8080)

The OpenAPI v3 specification is available at:

-   `http://localhost:8080/v3/api-docs`

## Development Workflow

1.  Make code changes.
2.  Write or update unit tests.
3.  Run `mvn clean install` to build and run tests.
4.  Run the application with the desired profile (e.g., `local`).
5.  Test endpoints using Swagger UI, cURL, or a REST client like Postman.

## Logging

-   Logging is handled by Log4j2, configured in `src/main/resources/log4j2.xml`.
-   **Local Profile**: Logs to console and to `./logs/weather-service-local.log`.
    -   `com.weather` package logs at `DEBUG` level.
    -   R2DBC and H2 logs at `DEBUG` for detailed local troubleshooting.
-   **Other Profiles (e.g., dev)**: Logs to console.
    -   `com.weather` package logs at `DEBUG` level.
    -   Framework logs (Spring, R2DBC) typically at `INFO`.

## Error Handling

-   A `GlobalErrorWebExceptionHandler` provides standardized JSON error responses.
-   Custom exceptions (`BaseException` and its subclasses) are used for specific error scenarios.
-   Validation errors (JSR-380) are also handled and returned in a structured format.
-   Error Response DTO: `com.weather.common.dto.ErrorResponse`.

## Resilience

Resilience patterns are implemented using Resilience4j:

-   **Circuit Breaker**: Applied to database operations and external service calls (if any).
-   **Retry**: Applied to database operations and external service calls.
-   **Rate Limiter**: Can be configured for specific endpoints or services.
-   **Time Limiter**: Can be configured for operations that might hang.

Configurations are in `application.yml` (global defaults and instance-specific) and `ResilienceConfig.java` (programmatic defaults). Health indicators for these components are available.

## Dockerization

(Instructions to be added once Dockerfile is finalized and tested)

---

This README provides a starting point. It should be updated as the project evolves.

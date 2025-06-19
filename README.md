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
- [Liveness and Readiness Probes](#liveness-and-readiness-probes)
- [Database Access and Inspection](#database-access-and-inspection)
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
   - **Deep Health**: http://localhost:8080/management/deephealth

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

## Liveness and Readiness Probes

The application includes **Kubernetes-style health probes** powered by Spring Boot Actuator for robust container orchestration and monitoring.

### 🔍 **Understanding the Probes**

When you access `/management/health`, you see:
```json
{"status":"UP","groups":["liveness","readiness"]}
```

#### **Liveness Probe** - *"Is my application alive?"*
- **Purpose**: Determines if the application process is running and responsive
- **Endpoint**: `/management/health/liveness`
- **Container Action**: If fails → **Container restart**
- **What it checks**: Basic application responsiveness (Spring context, JVM health)

```bash
curl http://localhost:8080/management/health/liveness
# Response: {"status":"UP"} or {"status":"DOWN"}
```

#### **Readiness Probe** - *"Is my application ready to serve traffic?"*
- **Purpose**: Determines if the application can handle requests properly
- **Endpoint**: `/management/health/readiness`
- **Container Action**: If fails → **Stop routing traffic** (no restart)
- **What it checks**: Database connections, external services, all dependencies

```bash
curl http://localhost:8080/management/health/readiness
# Response: {"status":"UP"} or {"status":"DOWN"}
```

### 🎯 **Real-World Scenarios**

| Scenario | Liveness | Readiness | Container Action |
|----------|----------|-----------|------------------|
| **App starting up** | UP | DOWN | No traffic until ready |
| **Normal operation** | UP | UP | Full traffic ✅ |
| **Database disconnected** | UP | DOWN | Stop traffic, no restart |
| **App crashed/frozen** | DOWN | DOWN | Restart container |
| **Memory leak/deadlock** | DOWN | UP/DOWN | Restart container |

### 🚀 **Benefits**

1. **Zero-downtime deployments** - Traffic switches only when new instances are ready
2. **Automatic recovery** - Unhealthy containers restart automatically  
3. **Traffic protection** - Users never hit broken instances
4. **Monitoring insights** - Clear visibility into application health states
5. **Kubernetes compatibility** - Works seamlessly with container orchestrators

### ⚙️ **Configuration**

Our application is configured in `application.yml`:
```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true
```

### 🔧 **Testing Probes Locally**

```bash
# Test both probes individually
curl http://localhost:8080/management/health/liveness
curl http://localhost:8080/management/health/readiness

# View detailed health information
curl "http://localhost:8080/management/health?show-details=always"

# Test what affects readiness (database, resilience components)
curl http://localhost:8080/management/deephealth
```

## Azure Container Service Integration

### **Azure Container Instances (ACI)**

```yaml
apiVersion: 2021-03-01
location: eastus
name: weather-service-aci
properties:
  containers:
  - name: weather-service
    properties:
      image: your-registry/weather-service:latest
      ports:
      - port: 8080
        protocol: TCP
      # Liveness Probe Configuration
      livenessProbe:
        httpGet:
          path: /management/health/liveness
          port: 8080
          scheme: HTTP
        initialDelaySeconds: 60    # Wait 60s after container start
        periodSeconds: 20          # Check every 20s
        timeoutSeconds: 5          # 5s timeout per check
        failureThreshold: 3        # Restart after 3 failures
      # Readiness Probe Configuration
      readinessProbe:
        httpGet:
          path: /management/health/readiness
          port: 8080
          scheme: HTTP
        initialDelaySeconds: 30    # Check readiness after 30s
        periodSeconds: 10          # Check every 10s
        timeoutSeconds: 5          # 5s timeout per check
        failureThreshold: 3        # Stop traffic after 3 failures
  osType: Linux
  restartPolicy: Always
```

### **Azure Container Apps**

```yaml
apiVersion: apps/v1alpha1
kind: ContainerApp
metadata:
  name: weather-service
spec:
  containers:
  - name: weather-service
    image: your-registry/weather-service:latest
    ports:
    - containerPort: 8080
      name: http
    probes:
    # Liveness Probe
    - type: liveness
      httpGet:
        path: "/management/health/liveness"
        port: 8080
      initialDelaySeconds: 60
      periodSeconds: 20
      timeoutSeconds: 5
      failureThreshold: 3
    # Readiness Probe
    - type: readiness
      httpGet:
        path: "/management/health/readiness"
        port: 8080
      initialDelaySeconds: 30
      periodSeconds: 10
      timeoutSeconds: 5
      failureThreshold: 3
```

### **Azure Kubernetes Service (AKS)**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: weather-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: weather-service
  template:
    metadata:
      labels:
        app: weather-service
    spec:
      containers:
      - name: weather-service
        image: your-registry/weather-service:latest
        ports:
        - containerPort: 8080
        # Startup Probe (recommended for Spring Boot)
        startupProbe:
          httpGet:
            path: /management/health/liveness
            port: 8080
          initialDelaySeconds: 15
          periodSeconds: 10
          failureThreshold: 30      # Allow 5 minutes for startup
        # Liveness Probe
        livenessProbe:
          httpGet:
            path: /management/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 20
          timeoutSeconds: 5
          failureThreshold: 3
        # Readiness Probe
        readinessProbe:
          httpGet:
            path: /management/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        resources:
          requests:
            cpu: 500m
            memory: 1Gi
          limits:
            cpu: 1000m
            memory: 2Gi
```

### **Azure App Service (Container)**

```bash
# Configure health check in Azure App Service
az webapp config set \
  --resource-group myResourceGroup \
  --name weather-service-app \
  --generic-configurations '{"healthCheckPath": "/management/health"}'
```

### **⏱️ Recommended Probe Timing for Spring Boot Applications**

| Probe Type | Initial Delay | Period | Timeout | Failure Threshold | Purpose |
|------------|---------------|--------|---------|-------------------|---------|
| **Startup** | 15s | 10s | 5s | 30 (5 min total) | Allow Spring Boot startup |
| **Liveness** | 60s | 20s | 5s | 3 | Detect hung/crashed app |
| **Readiness** | 30s | 10s | 5s | 3 | Manage traffic routing |

### **🎯 Azure Benefits**

1. **Automatic Scaling**: Azure can scale based on health status
2. **Load Balancing**: Traffic routes only to healthy instances  
3. **Rolling Updates**: Zero-downtime deployments with readiness checks
4. **Monitoring Integration**: Azure Monitor tracks probe failures
5. **Cost Optimization**: Unhealthy instances don't consume traffic resources
6. **Multi-region Failover**: Health status drives traffic distribution

### **📊 Monitoring in Azure**

```bash
# View container health in Azure CLI
az container show --resource-group myRG --name weather-service --query "containers[0].instanceView.currentState"

# Check AKS pod health
kubectl get pods -l app=weather-service
kubectl describe pod <pod-name>

# View Container Apps health
az containerapp revision list --name weather-service --resource-group myRG
```

### **⚠️ Handling Probe Failures & Restart Loops**

#### **Liveness Probe Restart Loop Prevention**

If liveness probes keep failing, Azure will continuously restart containers, creating an endless restart loop. Here's how to prevent and troubleshoot this:

**Common Causes of Restart Loops:**
1. **Startup takes too long** - Spring Boot needs time to initialize
2. **Resource constraints** - Insufficient CPU/memory
3. **Application errors** - Unhandled exceptions during startup
4. **Network issues** - Probe endpoint unreachable
5. **Misconfigured probe timing** - Too aggressive timing settings

#### **Prevention Strategies**

**1. Use Startup Probes (Recommended for Spring Boot)**
```yaml
# AKS Configuration
startupProbe:
  httpGet:
    path: /management/health/liveness
    port: 8080
  initialDelaySeconds: 15
  periodSeconds: 10
  failureThreshold: 30        # Allow 5 minutes for startup (30 × 10s)
  timeoutSeconds: 5

livenessProbe:
  httpGet:
    path: /management/health/liveness
    port: 8080
  initialDelaySeconds: 60     # Only start after startup probe succeeds
  periodSeconds: 30           # Less frequent checks
  failureThreshold: 5         # More tolerance (5 × 30s = 2.5min before restart)
  timeoutSeconds: 10
```

**2. Graceful Degradation Strategy**
Configure your liveness probe to be more forgiving than readiness:
```yaml
# Liveness: Only fail if app is truly dead
livenessProbe:
  failureThreshold: 5         # Wait 2.5 minutes before restart
  
# Readiness: Fail quickly to stop traffic
readinessProbe:
  failureThreshold: 3         # Stop traffic after 30 seconds
```

⚠️ **IMPORTANT:** `failureThreshold: 3` means "restart after 3 consecutive failed checks" - it does NOT limit total restart attempts!

**3. Resource Allocation**
```yaml
resources:
  requests:
    memory: "1Gi"             # Minimum for Spring Boot
    cpu: "500m"
  limits:
    memory: "2Gi"             # Prevent OOM kills
    cpu: "1000m"
```

#### **Troubleshooting Restart Loops**

**Step 1: Check Container Logs**
```bash
# Azure Container Instances
az container logs --resource-group myRG --name weather-service

# AKS
kubectl logs -l app=weather-service --previous  # Previous crashed instance
kubectl logs -l app=weather-service -f          # Follow current logs

# Container Apps
az containerapp logs show --name weather-service --resource-group myRG
```

**Step 2: Check Resource Usage**
```bash
# AKS resource usage
kubectl top pods -l app=weather-service

# Describe pod for events
kubectl describe pod <pod-name>
```

**Step 3: Test Probe Endpoints Manually**
```bash
# Port-forward to test locally (AKS)
kubectl port-forward pod/<pod-name> 8080:8080

# Test the probe endpoints
curl http://localhost:8080/management/health/liveness
curl http://localhost:8080/management/health/readiness
```

#### **Emergency Troubleshooting Configuration**

If stuck in restart loop, temporarily disable probes to debug:

```yaml
# Temporary debugging configuration
livenessProbe:
  httpGet:
    path: /management/health/liveness
    port: 8080
  initialDelaySeconds: 300    # Wait 5 minutes
  periodSeconds: 60           # Check every minute
  failureThreshold: 10        # Very tolerant
  timeoutSeconds: 30
```

#### **Application-Level Solutions**

**1. Health Check Endpoint Resilience**
Our application already implements this in `/management/health/liveness` - it only fails if the Spring context is truly broken.

**2. Graceful Shutdown**
```yaml
# In application.yml
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
server:
  shutdown: graceful
```

**3. JVM Optimization**
```yaml
# Container environment variables
environment:
  - name: JAVA_OPTS
    value: "-Xms1g -Xmx1g -XX:+UseG1GC -XX:+UseContainerSupport"
```

#### **Monitoring & Alerting**

Set up alerts for restart patterns:
```bash
# Azure Monitor alert for container restarts
az monitor metrics alert create \
  --name "weather-service-restart-alert" \
  --resource-group myRG \
  --scopes "/subscriptions/<sub-id>/resourceGroups/myRG/providers/Microsoft.ContainerInstance/containerGroups/weather-service" \
  --condition "count ContainerRestartCount > 3" \
  --description "Weather service restarting too frequently"
```

### **🛡️ Excessive Restart Prevention Strategies**

#### **1. Restart Policy Configuration**

**❌ Common Misconception:**
`failureThreshold: 3` does NOT limit total restarts to 3 attempts. It means:
- Check 3 times consecutively
- If all 3 fail → restart container  
- After restart → check 3 times again
- **Result: Endless restart loop!**

**✅ To Actually Limit Restarts:**

**Azure Container Instances (ACI):**
```yaml
# Limit restart attempts
properties:
  restartPolicy: OnFailure    # Options: Always, OnFailure, Never
  containers:
  - name: weather-service
    properties:
      # If container exits successfully (code 0), don't restart
      # Only restart on failure, but Azure will still respect probe failures
      
# ⚠️ Note: ACI doesn't have built-in restart limits
# You need external monitoring to stop excessive restarts
```

**Azure Kubernetes Service (AKS):**
```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  # ✅ This controls deployment rollout, not individual pod restarts
  progressDeadlineSeconds: 600    # Stop deployment after 10 minutes
  
  template:
    spec:
      # ⚠️ restartPolicy: Always means pods restart infinitely
      restartPolicy: Always
      containers:
      - name: weather-service
        # Use startup probe to prevent premature liveness failures
        startupProbe:
          failureThreshold: 30      # 5 minutes grace period (30 × 10s)
        livenessProbe:
          failureThreshold: 5       # 5 failed checks before restart (5 × 30s = 2.5min)
          periodSeconds: 30         # Check every 30s (not 10s)
          
# ✅ Kubernetes doesn't limit pod restart attempts by default
# Use external controllers or monitoring for restart limits
```

**Azure Container Apps:**
```yaml
# Configure restart limits in container app revision
properties:
  template:
    revisionSuffix: v1
    containers:
    - name: weather-service
      probes:
      - type: startup
        failureThreshold: 30        # Allow long startup time
      - type: liveness  
        failureThreshold: 5         # More tolerant
        periodSeconds: 30           # Less frequent checks
```

#### **2. Circuit Breaker Pattern for Restarts**

**Exponential Backoff Strategy:**
```yaml
# AKS with custom restart controller
apiVersion: v1
kind: ConfigMap
metadata:
  name: restart-policy
data:
  max-restarts-per-hour: "5"
  backoff-multiplier: "2"
  max-backoff-seconds: "300"
---
# Custom pod disruption budget
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: weather-service-pdb
spec:
  minAvailable: 1
  selector:
    matchLabels:
      app: weather-service
```

#### **3. Health Check Resilience**

**Make Health Endpoints More Robust:**
Our application already implements this, but here's what makes it resilient:

```java
// In our application, liveness only fails on critical issues
@Component
public class LivenessHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        try {
            // Only check critical application state
            // Don't check database, external services, etc.
            return Health.up()
                .withDetail("status", "Application context is alive")
                .build();
        } catch (Exception e) {
            // Only fail if Spring context is truly broken
            return Health.down()
                .withDetail("error", "Application context failure")
                .build();
        }
    }
}
```

#### **4. Resource-Based Prevention**

**Guaranteed Resources:**
```yaml
resources:
  requests:
    memory: "1.5Gi"           # Higher request to prevent OOM
    cpu: "750m"               # Adequate CPU for startup
  limits:
    memory: "3Gi"             # Room for growth
    cpu: "2000m"              # Allow bursts during startup
    
# JVM memory settings
env:
- name: JAVA_OPTS
  value: "-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

**Node Affinity (AKS):**
```yaml
affinity:
  nodeAffinity:
    requiredDuringSchedulingIgnoredDuringExecution:
      nodeSelectorTerms:
      - matchExpressions:
        - key: node-type
          operator: In
          values: ["memory-optimized"]    # Use appropriate node types
```

#### **5. Progressive Deployment Strategy**

**Blue-Green Deployment:**
```yaml
# Deploy new version alongside old
apiVersion: argoproj.io/v1alpha1
kind: Rollout
spec:
  strategy:
    blueGreen:
      prePromotionAnalysis:
        templates:
        - templateName: health-check
        args:
        - name: service-name
          value: weather-service
      scaleDownDelaySeconds: 30
      postPromotionAnalysis:
        templates:
        - templateName: health-check
```

**Canary Deployment:**
```yaml
# Gradual traffic shift
spec:
  strategy:
    canary:
      steps:
      - setWeight: 10
      - pause: {duration: 2m}
      - analysis:
          templates:
          - templateName: restart-rate-check
      - setWeight: 50
      - pause: {duration: 5m}
```

#### **6. Startup Optimization**

**Application Startup Improvements:**
```yaml
# In application.yml
spring:
  main:
    lazy-initialization: false    # Keep false for predictable startup
  jpa:
    defer-datasource-initialization: true
  sql:
    init:
      mode: always
      continue-on-error: false    # Fail fast if data init fails

# Parallel bean initialization
context:
  initializer:
    classes: com.weather.config.ParallelBeanInitializer
```

**Container Optimization:**
```dockerfile
# Multi-stage build for faster startup
FROM eclipse-temurin:21-jre-alpine AS runtime
COPY --from=build /app/target/weather-service.jar app.jar

# Optimize JVM for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:InitialRAMPercentage=70 -XX:MaxRAMPercentage=80"

# Pre-warm JVM
RUN java -XX:DumpLoadedClassList=classes.lst -jar app.jar --dry-run || true
```

#### **7. Monitoring & Circuit Breaking**

**Restart Rate Monitoring:**
```bash
# Azure Monitor query for restart rate
az monitor log-analytics query \
  --workspace "your-workspace-id" \
  --analytics-query "
    ContainerInstanceLog_CL
    | where ContainerName_s == 'weather-service'
    | where Message contains 'restart'
    | summarize RestartCount=count() by bin(TimeGenerated, 1h)
    | where RestartCount > 3
  "
```

**Automatic Circuit Breaker:**
```yaml
# AKS deployment with restart circuit breaker
apiVersion: apps/v1
kind: Deployment
spec:
  progressDeadlineSeconds: 600    # Stop trying after 10 minutes
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 1
      maxSurge: 1
```

#### **8. Emergency Stop Mechanisms**

**Manual Intervention:**
```bash
# Stop deployment if restarts are excessive
az container stop --resource-group myRG --name weather-service

# AKS: Scale down to investigate
kubectl scale deployment weather-service --replicas=0

# Container Apps: Disable revision
az containerapp revision deactivate --name weather-service --resource-group myRG
```

**Automated Circuit Breaker:**
```yaml
# Kubernetes CronJob to monitor and pause
apiVersion: batch/v1
kind: CronJob
metadata:
  name: restart-monitor
spec:
  schedule: "*/5 * * * *"    # Every 5 minutes
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: monitor
            image: kubectl:latest
            command:
            - /bin/sh
            - -c
            - |
              RESTARTS=$(kubectl get pods -l app=weather-service --no-headers | awk '{sum+=$4} END {print sum}')
              if [ "$RESTARTS" -gt 10 ]; then
                kubectl scale deployment weather-service --replicas=0
                echo "Emergency scaling down due to excessive restarts: $RESTARTS"
              fi
```

These strategies work together to prevent restart loops and give you control over when and how containers restart, ensuring stable service operation in Azure! 🛡️

The probes ensure your Weather Service runs reliably in Azure with automatic recovery, intelligent traffic routing, and seamless deployments! 🚀

## Database Access and Inspection

When running with the `local` profile, the application uses an H2 in-memory database. Since this is a **WebFlux (reactive) application**, the traditional H2 web console is not compatible.

### ⚠️ H2 Console Limitation
**H2 Console does not work with Spring WebFlux** - it requires a servlet-based web stack. Our application uses the reactive web stack for non-blocking operations.

### 🔍 Alternative Ways to Inspect Database

#### **Option 1: Use the Weather API Endpoints (Recommended)**
```bash
# Get all cities with weather data
curl http://localhost:8080/api/v1/weather/cities

# Get latest weather for a specific city
curl http://localhost:8080/api/v1/weather/city/New%20York/latest

# Get all weather data for a city (paginated)
curl "http://localhost:8080/api/v1/weather/city/New%20York?page=0&size=10"

# Get weather data by date range
curl "http://localhost:8080/api/v1/weather/range?start=2024-01-01T00:00:00&end=2024-12-31T23:59:59"
```

#### **Option 2: Use Swagger UI (Interactive)**
Navigate to **http://localhost:8080/swagger-ui.html** and use the interactive interface to:
- View all available endpoints
- Test API calls with sample data
- See response formats and schemas

#### **Option 3: External H2 Tools**
If you need SQL access, you can:
1. **Stop the application**
2. **Change to file-based H2** (instead of in-memory) by updating `application-local.yml`:
   ```yaml
   spring:
     r2dbc:
       url: r2dbc:h2:file:./data/testdb
     datasource:
       url: jdbc:h2:file:./data/testdb
   ```
3. **Use external H2 tools** like DBeaver or IntelliJ IDEA database tools

#### **Option 4: Add Debug Endpoints**
The application includes health endpoints that show database connectivity:
```bash
# Check database health
curl http://localhost:8080/management/health

# Deep health check with database details
curl http://localhost:8080/management/deephealth
```

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

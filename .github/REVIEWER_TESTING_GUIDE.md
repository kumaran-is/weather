# 🧪 Reviewer Testing Guide

## **🎯 Purpose**
This guide provides comprehensive testing instructions for reviewers to validate pull requests for the Weather Service project. Follow these steps to ensure changes work correctly and don't introduce regressions.

## **⚡ Quick Start Checklist**
For experienced reviewers, here's the essential testing flow:

```bash
# Quick validation (5-10 minutes)
git checkout <branch-name> && git pull origin <branch-name>
./mvnw clean test package
./mvnw spring-boot:run -Dspring-boot.run.profiles=local &
sleep 30  # Wait for startup
curl -s http://localhost:8080/management/health | jq '.status'  # Should be "UP"
curl -s http://localhost:8080/api/v1/weather/cities | jq 'length'  # Should return count
open http://localhost:8080/swagger-ui.html  # Verify UI loads
kill %1  # Stop application
```

## **📋 Detailed Testing Procedures**

### **1. 📥 Environment Setup**

#### **Prerequisites Check**
```bash
# Verify Java version (must be 21+)
java -version
# Expected: openjdk version "21.x.x"

# Verify Maven wrapper is executable
./mvnw --version

# Check available disk space (minimum 2GB recommended)
df -h .

# Ensure port 8080 is available
lsof -i :8080  # Should return nothing
```

#### **Project Setup**

1. Clone the repository (if not already done)

```bash
git clone https://github.com/kumaran-is/weather.git
cd weather
```   
2. Checkout the PR branch

```bash
git fetch origin
git checkout <branch-name>
```

3. Ensure you have the latest changes

```bash
git pull origin <branch-name>
```

#### **🔧 Build & Validation**
4. Clean previous builds

```bash
./mvnw clean install
```

5. Compile and run static analysis

```bash
./mvnw compile
```
6. Run all tests (unit + integration)

```bash
./mvnw test
```

7. Build the complete application

```bash
./mvnw package
```

8. Check branch information

```bash
git log --oneline -5  # Review recent commits
git diff main..HEAD --stat  # See changed files summary
```

### **2. 🔧 Build Validation**

#### **Clean Build Process**
```bash
# 1. Clean any previous builds
./mvnw clean
rm -rf target/  # Ensure complete cleanup

# 2. Compile without running tests (quick syntax check)
./mvnw compile -q
echo "Compile status: $?"  # Should be 0

# 3. Check for compilation warnings
./mvnw compile | grep -i warning

# 4. Validate generated sources (MapStruct)
ls -la target/generated-sources/annotations/com/weather/mapper/
```

#### **Dependency Analysis**
```bash
# 5. Check for dependency issues
./mvnw dependency:analyze -q

# 6. Verify no security vulnerabilities (if available)
# ./mvnw org.owasp:dependency-check-maven:check

# 7. Check dependency tree for conflicts
./mvnw dependency:tree | grep -i conflict
```

### **3. 🧪 Test Execution**

#### **Unit Tests**
```bash
# 8. Run unit tests with detailed output
./mvnw test -Dtest="*Test" --batch-mode

# 9. Check test coverage (if configured)
./mvnw jacoco:report  # If jacoco is configured

# 10. Verify specific test categories
./mvnw test -Dgroups=unit  # If using JUnit categories
```

#### **Integration Tests**
```bash
# 11. Run integration tests
./mvnw test -Dtest="*IT" --batch-mode

# 12. Verify test database setup (H2)
ls -la target/h2db/  # Check if H2 files are created
```

#### **Build Package**
```bash
# 13. Create executable JAR
./mvnw package -DskipTests=false

# 14. Verify JAR structure
jar -tf target/weather-service-*.jar | head -20

# 15. Check JAR size (should be reasonable, ~50-100MB)
ls -lh target/weather-service-*.jar
```

### **4. 🚀 Application Startup**

#### **Local Profile Testing**
```bash
# 16. Start application with local profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.jvmArguments="-XX:+AllowRedefinitionToAddDeleteMethods" &

# 17. Monitor startup logs
tail -f logs/weather-service.log &
TAIL_PID=$!

# 18. Wait for application to be ready
echo "Waiting for application startup..."
for i in {1..60}; do
  if curl -s http://localhost:8080/management/health >/dev/null 2>&1; then
    echo "Application started successfully in ${i} seconds"
    break
  fi
  sleep 1
done

# Kill log tail
kill $TAIL_PID 2>/dev/null
```

#### **Startup Validation**
```bash
# 19. Check startup success
curl -s http://localhost:8080/management/health | jq '.status'
# Expected: "UP"

# 20. Verify BlockHound integration (if applicable)
grep -i "blockhound" logs/weather-service.log
# Expected: "✅ BlockHound successfully installed in WARNING-ONLY mode"

# 21. Check for any startup errors
grep -i "error\|exception\|failed" logs/weather-service.log | head -10
```

### **5. ✅ Health & Monitoring Verification**

#### **Health Endpoints**
```bash
# 22. Test all health endpoints
echo "=== Basic Health ==="
curl -s http://localhost:8080/management/health | jq '.'

echo "=== Liveness Probe ==="
curl -s http://localhost:8080/management/health/liveness | jq '.'

echo "=== Readiness Probe ==="
curl -s http://localhost:8080/management/health/readiness | jq '.'

echo "=== Deep Health Check ==="
curl -s http://localhost:8080/management/deephealth | jq '.'

# 23. Verify specific health indicators
curl -s http://localhost:8080/management/health | jq '.components | keys'
# Expected: ["db", "diskSpace", "ping", etc.]
```

#### **Application Information**
```bash
# 24. Check application info
echo "=== Application Info ==="
curl -s http://localhost:8080/management/info | jq '.'

# 25. Verify metrics endpoint
curl -s http://localhost:8080/management/metrics | jq '.names[0:10]'

# 26. Check Prometheus metrics (if enabled)
curl -s http://localhost:8080/management/prometheus | head -20
```

### **6. 📊 API Functionality Testing**

#### **Core API Endpoints**
```bash
# 27. Test cities endpoint
echo "=== Testing Cities Endpoint ==="
CITIES_RESPONSE=$(curl -s http://localhost:8080/api/v1/weather/cities)
echo $CITIES_RESPONSE | jq '.'
CITIES_COUNT=$(echo $CITIES_RESPONSE | jq 'length')
echo "Cities found: $CITIES_COUNT"

# 28. Test weather by city
echo "=== Testing Weather by City ==="
curl -s "http://localhost:8080/api/v1/weather/city/New York" | jq '.'

# 29. Test pagination
echo "=== Testing Pagination ==="
curl -s "http://localhost:8080/api/v1/weather/city/London?page=0&size=2" | jq '.content | length'

# 30. Test latest weather
echo "=== Testing Latest Weather ==="
curl -s "http://localhost:8080/api/v1/weather/city/Tokyo/latest" | jq '.city'
```

#### **CRUD Operations**
```bash
# 31. Test POST (Create)
echo "=== Testing Create Weather Data ==="
CREATE_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/weather \
  -H "Content-Type: application/json" \
  -d '{
    "city": "TestCity",
    "temperature": 25.5,
    "humidity": 60.0,
    "description": "Sunny Test",
    "windSpeed": 10.2
  }')
echo $CREATE_RESPONSE | jq '.'
CREATED_ID=$(echo $CREATE_RESPONSE | jq -r '.id')

# 32. Test GET (Read)
echo "=== Testing Get Weather Data ==="
curl -s "http://localhost:8080/api/v1/weather/${CREATED_ID}" | jq '.'

# 33. Test PUT (Update)
echo "=== Testing Update Weather Data ==="
curl -s -X PUT "http://localhost:8080/api/v1/weather/${CREATED_ID}" \
  -H "Content-Type: application/json" \
  -d '{
    "city": "TestCity",
    "temperature": 30.0,
    "humidity": 65.0,
    "description": "Updated Test",
    "windSpeed": 12.0
  }' | jq '.'

# 34. Test DELETE
echo "=== Testing Delete Weather Data ==="
curl -s -X DELETE "http://localhost:8080/api/v1/weather/${CREATED_ID}"
echo "Delete completed"

# 35. Verify deletion
curl -s "http://localhost:8080/api/v1/weather/${CREATED_ID}" | jq '.'
# Should return 404 or null
```

### **7. 🌐 Web Interface Testing**

#### **Swagger UI Validation**
```bash
# 36. Open Swagger UI
echo "=== Opening Swagger UI ==="
if command -v open >/dev/null; then
  open http://localhost:8080/swagger-ui.html
elif command -v xdg-open >/dev/null; then
  xdg-open http://localhost:8080/swagger-ui.html
else
  echo "Please manually open: http://localhost:8080/swagger-ui.html"
fi

# 37. Test OpenAPI documentation endpoint
curl -s http://localhost:8080/v3/api-docs | jq '.info'
```

#### **Manual Swagger UI Checklist**
Verify in the browser:
- [ ] **Swagger UI loads without JavaScript errors**
- [ ] **All API endpoints are listed and documented**
- [ ] **Example values are present in request schemas**
- [ ] **Response schemas are properly displayed**
- [ ] **Try executing a simple GET request (e.g., /cities)**
- [ ] **Authentication section is properly configured (if applicable)**

### **8. 🔍 Log Analysis**

#### **Log Validation**
```bash
# 38. Check for warnings and errors
echo "=== Checking for Errors ==="
grep -i "error" logs/weather-service.log | tail -10

echo "=== Checking for Warnings ==="
grep -i "warn" logs/weather-service.log | tail -10

echo "=== Checking BlockHound Violations ==="
grep "BLOCKHOUND_VIOLATION" logs/weather-service.log

# 39. Verify log structure (JSON format)
tail -5 logs/weather-service.log | jq '.' 2>/dev/null && echo "✅ JSON logging working" || echo "❌ JSON logging issue"

# 40. Check log levels
grep -c "INFO" logs/weather-service.log
grep -c "DEBUG" logs/weather-service.log
grep -c "ERROR" logs/weather-service.log
```

### **9. ⚡ Performance & Resource Monitoring**

#### **Resource Usage Check**
```bash
# 41. Check memory usage
echo "=== Memory Usage ==="
JAVA_PID=$(pgrep -f "weather-service")
if [ ! -z "$JAVA_PID" ]; then
  ps -p $JAVA_PID -o pid,ppid,rss,vsz,pmem,pcpu,comm
  echo "Memory usage (RSS): $(ps -p $JAVA_PID -o rss= | tr -d ' ') KB"
fi

# 42. Check JVM metrics via actuator
curl -s http://localhost:8080/management/metrics/jvm.memory.used | jq '.'
curl -s http://localhost:8080/management/metrics/jvm.threads.live | jq '.'

# 43. Monitor response times
echo "=== Response Time Test ==="
time curl -s http://localhost:8080/api/v1/weather/cities >/dev/null
```

### **10. 🛡️ Environment Profile Testing** (if applicable)

#### **Development Profile**
```bash
# 44. Test dev profile (if SQL Server configuration available)
# Note: This requires SQL Server to be running
echo "=== Testing Dev Profile (Optional) ==="
# ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev &
# sleep 30
# curl -s http://localhost:8080/management/health | jq '.status'
# kill %1
```

### **11. 🔄 Cleanup & Shutdown**

#### **Graceful Shutdown**
```bash
# 45. Test graceful shutdown
echo "=== Testing Graceful Shutdown ==="
JAVA_PID=$(pgrep -f "weather-service")
if [ ! -z "$JAVA_PID" ]; then
  kill -TERM $JAVA_PID
  sleep 10
  if pgrep -f "weather-service" >/dev/null; then
    echo "❌ Application did not shut down gracefully"
    kill -KILL $JAVA_PID
  else
    echo "✅ Application shut down gracefully"
  fi
fi

# 46. Clean up workspace
./mvnw clean -q
rm -f logs/weather-service.log
```

## **📋 Final Verification Checklist**

After completing all testing steps, verify the following:

### **✅ Build & Compilation**
- [ ] **Clean build succeeds** without errors
- [ ] **All tests pass** (unit and integration)
- [ ] **No compilation warnings** or minimal/expected warnings
- [ ] **JAR builds successfully** and is reasonable size

### **✅ Application Startup**
- [ ] **Starts within 30 seconds** on local profile
- [ ] **All health checks return UP** status
- [ ] **No ERROR messages** in logs during startup
- [ ] **BlockHound loads successfully** (if applicable)

### **✅ API Functionality**
- [ ] **All existing endpoints work** as expected
- [ ] **New endpoints function correctly** (if PR adds them)
- [ ] **CRUD operations complete** successfully
- [ ] **Pagination works** properly
- [ ] **Error handling** returns appropriate status codes

### **✅ Web Interface**
- [ ] **Swagger UI loads** without errors
- [ ] **API documentation is complete** and accurate
- [ ] **Example values work** in Swagger UI
- [ ] **API execution through Swagger** functions

### **✅ Performance & Resources**
- [ ] **Memory usage reasonable** (<1GB for local)
- [ ] **Startup time acceptable** (<30 seconds)
- [ ] **Response times good** (<500ms for simple endpoints)
- [ ] **No memory leaks** during basic operations

### **✅ Logs & Monitoring**
- [ ] **Structured logging works** (JSON format)
- [ ] **Log levels appropriate** (DEBUG in local, minimal ERRORs)
- [ ] **No unexpected warnings** or errors
- [ ] **Monitoring endpoints accessible**

## **🚨 Red Flags - Stop Review If Found**

- **❌ Application fails to start** after multiple attempts
- **❌ Health checks consistently fail**
- **❌ Critical errors** in logs during normal operation
- **❌ Memory usage > 2GB** for simple local testing
- **❌ Core API endpoints return 500 errors**
- **❌ Database connectivity issues** with H2
- **❌ Tests fail** consistently

## **📞 Getting Help**

If you encounter issues during testing:

1. **Check troubleshooting section** in main README.md
2. **Review common issues** in this guide
3. **Ask the PR author** for clarification
4. **Check project documentation** in `/docs` folder
5. **Create an issue** if it's a broader problem

---

**🧪 This testing guide ensures thorough validation of all Weather Service pull requests while maintaining high quality standards.**
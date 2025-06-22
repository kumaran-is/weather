# 🚀 Pull Request

## **📋 Summary**
<!-- Provide a brief description of what this PR accomplishes -->

### **🎯 Type of Change**
- [ ] 🐛 Bug fix (non-breaking change which fixes an issue)
- [ ] ✨ New feature (non-breaking change which adds functionality)
- [ ] 💥 Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] 📚 Documentation only changes
- [ ] 🔧 Configuration changes (environment, dependencies, etc.)
- [ ] ♻️ Code refactoring (no functional changes, no api changes)
- [ ] ⚡ Performance improvements
- [ ] 🧪 Test improvements
- [ ] 🔨 Build system changes

## **📖 Description**
<!-- Provide a detailed description of the changes -->

### **💡 Motivation and Context**
<!-- Why is this change required? What problem does it solve? -->
<!-- If it fixes an open issue, please link to the issue here -->

### **🔄 Changes Made**
<!-- List the main changes made in this PR -->
- 
- 
- 

## **🧪 Testing**

### **✅ Test Coverage**
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing completed
- [ ] All existing tests pass

### **🔍 Testing Checklist**
- [ ] Local environment tested (`./mvnw spring-boot:run -Dspring-boot.run.profiles=local`)
- [ ] Development environment compatibility verified
- [ ] Health checks pass (`/management/health`)
- [ ] API endpoints tested (if applicable)
- [ ] BlockHound violations checked (if reactive code changed)
- [ ] Performance impact assessed (if applicable)

### **🧪 Reviewer Testing Instructions**

#### **📥 Setup & Checkout**
```bash
# 1. Clone the repository (if not already done)
git clone https://github.com/your-org/weather-service.git
cd weather-service

# 2. Checkout the PR branch
git fetch origin
git checkout <branch-name>

# 3. Ensure you have the latest changes
git pull origin <branch-name>
```

#### **🔧 Build & Validation**
```bash
# 4. Clean previous builds
./mvnw clean

# 5. Compile and run static analysis
./mvnw compile

# 6. Run all tests (unit + integration)
./mvnw test

# 7. Build the complete application
./mvnw package

# 8. Check for any compilation warnings or errors
```

#### **🚀 Local Testing**
```bash
# 9. Start the application with local profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Wait for startup completion (look for "Started WeatherServiceApplication")
```

#### **✅ Health & Functionality Verification**
```bash
# 10. Test health endpoints (in a new terminal)
curl -s http://localhost:8080/management/health | jq '.'
curl -s http://localhost:8080/management/health/liveness | jq '.'
curl -s http://localhost:8080/management/health/readiness | jq '.'

# 11. Test deep health check
curl -s http://localhost:8080/management/deephealth | jq '.'

# 12. Verify application info
curl -s http://localhost:8080/management/info | jq '.'
```

#### **📊 API Testing**
```bash
# 13. Test core API endpoints
# Get all cities
curl -s http://localhost:8080/api/v1/weather/cities | jq '.'

# Get weather by city (should return data from H2)
curl -s "http://localhost:8080/api/v1/weather/city/New York" | jq '.'

# Test pagination
curl -s "http://localhost:8080/api/v1/weather/city/London?page=0&size=5" | jq '.'

# Create new weather data (POST test)
curl -X POST http://localhost:8080/api/v1/weather \
  -H "Content-Type: application/json" \
  -d '{
    "city": "TestCity",
    "temperature": 25.5,
    "humidity": 60.0,
    "description": "Sunny",
    "windSpeed": 10.2
  }' | jq '.'
```

#### **🌐 Swagger UI Testing**
```bash
# 14. Open Swagger UI in browser
open http://localhost:8080/swagger-ui.html
# Or manually navigate to: http://localhost:8080/swagger-ui.html

# Verify:
# - [ ] Swagger UI loads without errors
# - [ ] All endpoints are documented
# - [ ] Example values are present
# - [ ] Try executing a GET request through Swagger UI
# - [ ] Response schemas are properly displayed
```

#### **🔍 Logs & Monitoring**
```bash
# 15. Check application logs
tail -f logs/weather-service.log

# Look for:
# - [ ] No ERROR level messages
# - [ ] INFO level startup messages
# - [ ] BlockHound integration message (if applicable)
# - [ ] No [BLOCKHOUND_VIOLATION] warnings (unless expected)
```

#### **⚡ Performance & Resource Check**
```bash
# 16. Monitor resource usage
# Check memory usage
jps -v | grep weather

# Check process CPU/memory (Mac/Linux)
top -p $(pgrep -f weather-service)

# Verify reasonable startup time (<30 seconds for local)
```

#### **🛡️ Environment Profile Testing** (if configuration changed)
```bash
# 17. Test different profiles (if applicable)
# Stop current instance (Ctrl+C) then test:

# Dev profile (requires SQL Server or mock)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Check health after profile change
curl -s http://localhost:8080/management/health | jq '.'
```

#### **🔄 Cleanup**
```bash
# 18. Stop the application
# Press Ctrl+C in the terminal running the application

# 19. Clean workspace (optional)
./mvnw clean
```

### **📋 Reviewer Verification Checklist**
After completing the testing steps above, verify:

- [ ] **Application starts successfully** (within 30 seconds)
- [ ] **All health checks return UP** status
- [ ] **No error messages** in startup logs
- [ ] **Swagger UI loads and functions** properly
- [ ] **Core API endpoints respond** correctly
- [ ] **Sample data is accessible** (for local profile)
- [ ] **No BlockHound violations** (unless documented)
- [ ] **Resource usage is reasonable** (memory < 1GB for local)
- [ ] **Application stops gracefully** (Ctrl+C)

### **🚨 Common Issues & Solutions**
| Issue | Symptom | Solution |
|-------|---------|----------|
| **Port already in use** | `Port 8080 was already in use` | Kill existing process: `lsof -ti:8080 \| xargs kill` |
| **Java version mismatch** | `UnsupportedClassVersionError` | Ensure Java 21: `java -version` |
| **BlockHound error** | `IllegalStateException` | Check JVM args in pom.xml |
| **Database connection** | Health check fails | Verify H2 file permissions |
| **Memory issues** | `OutOfMemoryError` | Increase heap: `-Xmx2g` |

### **📋 Test Scenarios**
<!-- Describe the testing scenarios covered -->

## **📝 Checklist**

### **🔧 Code Quality**
- [ ] Code follows project coding standards
- [ ] Self-review of code completed
- [ ] Code is properly commented, particularly in hard-to-understand areas
- [ ] No debug statements or TODO comments left in production code
- [ ] Proper error handling implemented
- [ ] Reactive programming best practices followed

### **📚 Documentation**
- [ ] Documentation updated (if needed)
- [ ] API documentation updated (if endpoints changed)
- [ ] CHANGELOG.md updated (if applicable)
- [ ] README.md updated (if setup/usage changed)

### **🛡️ Security & Performance**
- [ ] No sensitive information exposed in logs or code
- [ ] Performance impact considered and documented
- [ ] Memory usage implications assessed
- [ ] No blocking calls introduced in reactive code paths
- [ ] Proper input validation implemented

### **🔄 Configuration & Dependencies**
- [ ] Configuration changes documented
- [ ] Environment variables updated (if needed)
- [ ] Dependencies are up-to-date and secure
- [ ] Database migration scripts provided (if needed)

## **🏗️ Architecture & Design**

### **📐 Design Patterns**
- [ ] Follows hexagonal architecture principles
- [ ] Proper separation of concerns maintained
- [ ] Reactive patterns used appropriately
- [ ] Error handling follows established patterns

### **🔗 Dependencies**
<!-- List any new dependencies or major dependency updates -->

## **📸 Screenshots/Demo**
<!-- If applicable, add screenshots or demo links -->

## **🚨 Breaking Changes**
<!-- If this is a breaking change, describe the impact and migration steps -->

## **📊 Performance Impact**
<!-- Describe any performance implications -->

## **🔍 Reviewer Notes**
<!-- Any specific areas you'd like reviewers to focus on -->

## **🎯 Related Issues**
<!-- Link to related issues -->
Closes #
Related to #

---

### **📋 Post-Merge Checklist**
- [ ] Deployment verified in staging environment
- [ ] Monitoring alerts checked
- [ ] Performance metrics reviewed
- [ ] Documentation deployed
- [ ] Team notified of changes (if needed)

---

**⚡ Generated with [Weather Service PR Template](https://github.com/weather-service/weather/blob/main/.github/pull_request_template.md)**
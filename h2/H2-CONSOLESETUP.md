# H2 Console Setup for Spring Boot WebFlux - Local Development Guide

## Table of Contents
- [H2 Console Setup for Spring Boot WebFlux - Local Development Guide](#h2-console-setup-for-spring-boot-webflux---local-development-guide)
  - [Table of Contents](#table-of-contents)
  - [Overview](#overview)
  - [Problem: Why Embedded H2 Console Doesn't Work with WebFlux](#problem-why-embedded-h2-console-doesnt-work-with-webflux)
    - [Technical Background](#technical-background)
    - [Error Symptoms](#error-symptoms)
  - [Solution: Standalone H2 Console + File-Based Database](#solution-standalone-h2-console--file-based-database)
    - [Why This Approach Works](#why-this-approach-works)
    - [Benefits](#benefits)
      - [✅ **Developer Experience**](#-developer-experience)
      - [✅ **Development Workflow**](#-development-workflow)
      - [✅ **Team Benefits**](#-team-benefits)
  - [Setup Instructions](#setup-instructions)
    - [Prerequisites](#prerequisites)
    - [Step 1: Configure application-local.yml](#step-1-configure-application-localyml)
    - [Step 2: Update .gitignore](#step-2-update-gitignore)
    - [Step 3: Maven Configuration](#step-3-maven-configuration)
  - [Daily Development Workflow](#daily-development-workflow)
    - [Starting Your Development Environment](#starting-your-development-environment)
    - [Accessing H2 Console](#accessing-h2-console)
    - [Port Configuration](#port-configuration)
  - [Convenience Scripts](#convenience-scripts)
    - [For Mac/Linux Teams](#for-maclinux-teams)
    - [For Windows Teams](#for-windows-teams)
  - [Troubleshooting](#troubleshooting)
    - [Common Issues and Solutions](#common-issues-and-solutions)
      - [Issue: "Wrong user name or password"](#issue-wrong-user-name-or-password)
      - [Issue: "Database not found"](#issue-database-not-found)
      - [Issue: Port conflicts](#issue-port-conflicts)
      - [Issue: Database file locked](#issue-database-file-locked)
    - [Verification Checklist](#verification-checklist)
  - [Best Practices](#best-practices)
    - [Development Guidelines](#development-guidelines)
    - [Team Coordination](#team-coordination)
  - [Alternative Environments](#alternative-environments)
  - [Support](#support)

## Overview

This document explains why we use a standalone H2 Console with file-based H2 database for local development in our Spring Boot WebFlux application, and provides step-by-step setup instructions for the team.

## Problem: Why Embedded H2 Console Doesn't Work with WebFlux

### Technical Background

Spring Boot's embedded H2 Console (`spring.h2.console.enabled=true`) has known compatibility issues with WebFlux applications:

1. **Servlet vs Reactive Stack Conflict**: The embedded H2 Console is built on the traditional Servlet stack, while WebFlux runs on the reactive Netty server
2. **Request Handling Incompatibility**: The console expects synchronous request/response handling, but WebFlux uses asynchronous, non-blocking I/O
3. **Context Path Issues**: The embedded console doesn't properly integrate with WebFlux's routing mechanism
4. **Session Management**: Different session handling approaches between Servlet and Reactive stacks

### Error Symptoms

When attempting to use embedded H2 Console with WebFlux, you typically encounter:
- Console page loads but database connections fail
- "Wrong user name or password" errors even with correct credentials
- Browser timeouts or hanging connections
- ClassPath conflicts between Servlet and Reactive dependencies

## Solution: Standalone H2 Console + File-Based Database

### Why This Approach Works

1. **Separation of Concerns**: Console runs as independent process, avoiding stack conflicts
2. **TCP Server Access**: File-based H2 supports `AUTO_SERVER` mode for external connections
3. **True Database Persistence**: Data survives application restarts (useful for development)
4. **Full Feature Access**: Complete H2 Console functionality without limitations
5. **Team Collaboration**: Multiple developers can access the same database instance

### Benefits

#### ✅ **Developer Experience**
- Full database browsing capabilities
- Real-time query execution
- Schema inspection and modification
- Data manipulation through GUI
- No application restart required for database access

#### ✅ **Development Workflow**
- Persistent data across application restarts
- Ability to prepare test data sets
- Easy debugging of database-related issues
- SQL query testing and optimization

#### ✅ **Team Benefits**
- Consistent development environment
- Shared understanding of database setup
- Easy onboarding for new team members
- No platform-specific issues

## Setup Instructions

### Prerequisites

- Java 21+ installed
- Maven configured
- H2 JAR file available in project (location: `h2/h2.jar`), you can download the latest jar from [H2 official website](https://www.h2database.com/html/download.html)

### Step 1: Configure application-local.yml

Create or update your `src/main/resources/application-local.yml`:

```yaml
spring:
  r2dbc:
    # File-based H2 with AUTO_SERVER for external console access
    url: r2dbc:h2:file:///./target/h2db/weatherdb?options=AUTO_SERVER=TRUE;AUTO_SERVER_PORT=9092;DB_CLOSE_DELAY=-1
    username: sa
    password:  # Leave empty - no password for local development
    pool:
      enabled: true
      initial-size: 3
      max-size: 10
      max-idle-time: 10m
      max-life-time: 30m
      max-acquire-time: 2s
      max-create-connection-time: 3s
      validation-query: SELECT 1
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql

logging:
  file:
    name: ./logs/weather-service-local.log
  level:
    com.weather: DEBUG
    org.springframework.r2dbc: DEBUG
    io.r2dbc.h2: DEBUG
    io.r2dbc.pool: DEBUG
```

### Step 2: Update .gitignore

Add these entries to your `.gitignore`:

```gitignore
# H2 Database files (local development)
/target/h2db/
*.db
*.trace.db
*.lock.db

# H2 Console logs
h2-console.log
```

### Step 3: Maven Configuration

Ensure H2 dependencies are properly configured in `pom.xml`:

```xml
<dependencies>
    <!-- H2 Database - compile scope for local development -->
    <!-- R2DBC Drivers -->
    <dependency>
        <groupId>io.r2dbc</groupId>
        <artifactId>r2dbc-h2</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

For production builds, configure exclusions:

```xml
<profiles>
    <profile>
        <id>prod</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <configuration>
                        <excludes>
                            <exclude>
                                <groupId>com.h2database</groupId>
                                <artifactId>h2</artifactId>
                            </exclude>
                            <exclude>
                                <groupId>io.r2dbc</groupId>
                                <artifactId>r2dbc-h2</artifactId>
                            </exclude>
                        </excludes>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

## Daily Development Workflow

### Starting Your Development Environment

**Terminal 1: Start Spring Boot Application**
```bash
# From project root directory
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Terminal 2: Start H2 Console**
- Run from project root directory:

```bash
java -cp h2/h2.jar org.h2.tools.Console -web -webPort 8082
```

or

**MAC/Linux**
- Make script executable
- Run from project root directory:

```bash
chmod +x start-h2-console.sh
```

```bash
./start-h2-console.sh
```

**Windows**
- Run from project root directory:

```cmd
start-h2-console.bat
```

### Accessing H2 Console

1. Open browser to: `http://localhost:8082`
2. Use these connection settings:
   - **JDBC URL**: `jdbc:h2:./target/h2db/weatherdb;AUTO_SERVER=TRUE`
   - **Username**: `sa`
   - **Password**: (leave empty)
   - **Driver Class**: `org.h2.Driver` (auto-populated)
3. Click "Connect"

### Port Configuration

| Service | Port | Purpose |
|---------|------|---------|
| Spring Boot WebFlux | 8080 | Main application |
| H2 Console Web UI | 8082 | Database administration |
| H2 TCP Server | 9092 | Database connections |

## Convenience Scripts

### For Mac/Linux Teams

Create `start-h2-console.sh`:
```bash
#!/bin/bash
echo "Starting H2 Console on port 8082..."
echo "Access at: http://localhost:8082"
echo "JDBC URL: jdbc:h2:./target/h2db/weatherdb;AUTO_SERVER=TRUE"
echo "Username: sa"
echo "Password: (leave empty)"
java -cp h2/h2.jar org.h2.tools.Console -web -webPort 8082
```

Make executable: `chmod +x start-h2-console.sh`

### For Windows Teams

Create `start-h2-console.bat`:
```batch
@echo off
echo Starting H2 Console on port 8082...
echo Access at: http://localhost:8082
echo JDBC URL: jdbc:h2:./target/h2db/weatherdb;AUTO_SERVER=TRUE
echo Username: sa
echo Password: (leave empty)
java -cp h2\h2.jar org.h2.tools.Console -web -webPort 8082
```

## Troubleshooting

### Common Issues and Solutions

#### Issue: "Wrong user name or password"
**Solution**: Ensure you're using the exact JDBC URL format:
`jdbc:h2:./target/h2db/weatherdb;AUTO_SERVER=TRUE`

#### Issue: "Database not found"
**Solution**: 
1. Verify Spring Boot application started successfully
2. Check that `target/h2db/` directory exists with `.mv.db` files
3. Ensure application-local.yml profile is active

#### Issue: Port conflicts
**Solution**: 
- Check if ports 8080, 8082, or 9092 are in use
- Change H2 Console port: `-webPort 8083`
- Change TCP server port in configuration

#### Issue: Database file locked
**Solution**:
1. Stop all H2 Console instances
2. Stop Spring Boot application
3. Delete `.lock.db` files in `target/h2db/`
4. Restart application and console

### Verification Checklist

- [ ] Spring Boot application starts without errors
- [ ] H2 database files created in `target/h2db/`
- [ ] H2 Console accessible at `http://localhost:8082`
- [ ] Database connection successful with provided credentials
- [ ] Schema tables visible in console
- [ ] Sample data loaded correctly

## Best Practices

### Development Guidelines

1. **Always use `local` profile** for this setup
2. **Don't commit database files** - they're in `.gitignore`
3. **Use meaningful test data** in `data.sql` for team consistency
4. **Document schema changes** in `schema.sql`
5. **Clean target directory** occasionally: `mvn clean`

### Team Coordination

1. **Standardize test data**: Keep `data.sql` updated with useful test scenarios
2. **Share schema updates**: Coordinate changes to `schema.sql`
3. **Document queries**: Save useful SQL queries in team knowledge base
4. **Version control**: Only commit configuration files, not database files

## Alternative Environments

This setup is **only for local development**. Other environments use:

- **Dev**: SQL Server with connection pooling
- **QA**: PostgreSQL or Azure SQL
- **Prod**: PostgreSQL or Azure SQL with high availability

The file-based H2 approach ensures local development doesn't interfere with shared environments while providing full database functionality for development and debugging.

## Support

If you encounter issues with this setup:

1. Check this troubleshooting guide first
2. Verify your Java and Maven versions
3. Ensure all prerequisites are met
4. Reach out to the team for assistance


#!/bin/bash
echo "Starting H2 Console on port 8082..."
echo "Access at: http://localhost:8082"
echo "JDBC URL: jdbc:h2:./target/h2db/weatherdb;AUTO_SERVER=TRUE"
echo "Username: sa"
echo "Password: (leave empty)"

# Check if H2 JAR exists in expected location
if [ -f "h2/h2.jar" ]; then
    echo "Found H2 JAR at h2/h2.jar"
    java -cp h2/h2.jar org.h2.tools.Console -web -webPort 8082
elif [ -f "h2/h2-*.jar" ]; then
    echo "Found H2 JAR at h2/h2-*.jar"
    java -cp h2/h2-*.jar org.h2.tools.Console -web -webPort 8082
else
    echo "ERROR: H2 JAR not found in h2/ directory"
    echo "Please ensure h2.jar is placed in the h2/ directory"
    echo "Or download it from: https://www.h2database.com/html/download.html"
    exit 1
fi
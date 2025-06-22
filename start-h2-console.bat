@echo off
echo Starting H2 Console on port 8082...
echo Access at: http://localhost:8082
echo JDBC URL: jdbc:h2:./target/h2db/weatherdb;AUTO_SERVER=TRUE
echo Username: sa
echo Password: (leave empty)
java -cp h2\h2.jar org.h2.tools.Console -web -webPort 8082

package com.weather;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Slf4j
@SpringBootApplication
@EnableR2dbcRepositories
@EnableR2dbcAuditing
@OpenAPIDefinition(
        info = @Info(
                title = "Weather Service API",
                version = "1.0.0",
                description = "A reactive REST service for managing weather data",
                contact = @Contact(
                        name = "Weather Service Team",
                        email = "weather-service@example.com"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local server"),
                @Server(url = "https://api.weather-service.com", description = "Production server")
        }
)
public class WeatherServiceApplication {

    public static void main(String[] args) {
        log.info("Starting Weather Service Application...");
        SpringApplication.run(WeatherServiceApplication.class, args);
        log.info("Weather Service Application started successfully!");
    }
}
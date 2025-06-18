package com.weather.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Weather data request")
public class WeatherDataRequest {
    
    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City name must not exceed 100 characters")
    @Schema(description = "City name", example = "New York", required = true)
    private String city;
    
    @Size(max = 100, message = "Country name must not exceed 100 characters")
    @Schema(description = "Country name", example = "USA")
    private String country;
    
    @NotNull(message = "Temperature is required")
    @DecimalMin(value = "-100.00", message = "Temperature must be greater than -100°C")
    @DecimalMax(value = "100.00", message = "Temperature must be less than 100°C")
    @Schema(description = "Temperature in Celsius", example = "22.5", required = true)
    private BigDecimal temperature;
    
    @Min(value = 0, message = "Humidity must be between 0 and 100")
    @Max(value = 100, message = "Humidity must be between 0 and 100")
    @Schema(description = "Humidity percentage", example = "65")
    private Integer humidity;
    
    @DecimalMin(value = "0.00", message = "Pressure must be positive")
    @DecimalMax(value = "2000.00", message = "Pressure must be less than 2000 hPa")
    @Schema(description = "Atmospheric pressure in hPa", example = "1013.25")
    private BigDecimal pressure;
    
    @DecimalMin(value = "0.00", message = "Wind speed must be positive")
    @DecimalMax(value = "500.00", message = "Wind speed must be less than 500 km/h")
    @Schema(description = "Wind speed in km/h", example = "5.2")
    private BigDecimal windSpeed;
    
    @Size(max = 3, message = "Wind direction must not exceed 3 characters")
    @Pattern(regexp = "^(N|NE|E|SE|S|SW|W|NW)$", message = "Wind direction must be a valid compass direction")
    @Schema(description = "Wind direction", example = "NW")
    private String windDirection;
    
    @Size(max = 50, message = "Weather condition must not exceed 50 characters")
    @Schema(description = "Weather condition", example = "Clear")
    private String weatherCondition;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(description = "Weather description", example = "Clear skies with light winds")
    private String description;
    
    @NotNull(message = "Recorded time is required")
    @Schema(description = "When the weather was recorded", example = "2024-01-15T10:00:00", required = true)
    private LocalDateTime recordedAt;
}
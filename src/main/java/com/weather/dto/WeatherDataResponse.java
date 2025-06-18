package com.weather.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Weather data response")
public record WeatherDataResponse(
    
    @Schema(description = "Weather record ID", example = "1")
    Long id,
    
    @Schema(description = "City name", example = "New York")
    String city,
    
    @Schema(description = "Country name", example = "USA")
    String country,
    
    @Schema(description = "Temperature in Celsius", example = "22.5")
    BigDecimal temperature,
    
    @Schema(description = "Humidity percentage", example = "65")
    Integer humidity,
    
    @Schema(description = "Atmospheric pressure in hPa", example = "1013.25")
    BigDecimal pressure,
    
    @Schema(description = "Wind speed in km/h", example = "5.2")
    BigDecimal windSpeed,
    
    @Schema(description = "Wind direction", example = "NW")
    String windDirection,
    
    @Schema(description = "Weather condition", example = "Clear")
    String weatherCondition,
    
    @Schema(description = "Weather description", example = "Clear skies with light winds")
    String description,
    
    @Schema(description = "When the weather was recorded", example = "2024-01-15T10:00:00")
    LocalDateTime recordedAt,
    
    @Schema(description = "When the record was created", example = "2024-01-15T10:00:00")
    LocalDateTime createdAt,
    
    @Schema(description = "When the record was last updated", example = "2024-01-15T10:00:00")
    LocalDateTime updatedAt
) {}
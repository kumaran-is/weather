package com.weather.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Weather data response")
public class WeatherDataResponse {
    
    @Schema(description = "Weather record ID", example = "1")
    private Long id;
    
    @Schema(description = "City name", example = "New York")
    private String city;
    
    @Schema(description = "Country name", example = "USA")
    private String country;
    
    @Schema(description = "Temperature in Celsius", example = "22.5")
    private BigDecimal temperature;
    
    @Schema(description = "Humidity percentage", example = "65")
    private Integer humidity;
    
    @Schema(description = "Atmospheric pressure in hPa", example = "1013.25")
    private BigDecimal pressure;
    
    @Schema(description = "Wind speed in km/h", example = "5.2")
    private BigDecimal windSpeed;
    
    @Schema(description = "Wind direction", example = "NW")
    private String windDirection;
    
    @Schema(description = "Weather condition", example = "Clear")
    private String weatherCondition;
    
    @Schema(description = "Weather description", example = "Clear skies with light winds")
    private String description;
    
    @Schema(description = "When the weather was recorded", example = "2024-01-15T10:00:00")
    private LocalDateTime recordedAt;
    
    @Schema(description = "When the record was created", example = "2024-01-15T10:00:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "When the record was last updated", example = "2024-01-15T10:00:00")
    private LocalDateTime updatedAt;
}
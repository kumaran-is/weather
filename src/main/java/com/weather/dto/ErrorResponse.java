package com.weather.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Error response")
public record ErrorResponse(
    
    @Schema(description = "Error code", example = "WEATHER_NOT_FOUND")
    String code,
    
    @Schema(description = "Error message", example = "Weather data not found")
    String message,
    
    @Schema(description = "HTTP status code", example = "404")
    int status,
    
    @Schema(description = "Request path", example = "/api/v1/weather/123")
    String path,
    
    @Schema(description = "Timestamp when error occurred")
    LocalDateTime timestamp,
    
    @Schema(description = "Validation errors")
    List<ValidationError> validationErrors
) {
    
    @Schema(description = "Validation error details")
    public record ValidationError(
        
        @Schema(description = "Field name", example = "temperature")
        String field,
        
        @Schema(description = "Rejected value", example = "150")
        Object rejectedValue,
        
        @Schema(description = "Error message", example = "Temperature must be less than 100°C")
        String message
    ) {}
}
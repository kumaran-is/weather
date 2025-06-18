package com.weather.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Error response")
public class ErrorResponse {
    
    @Schema(description = "Error code", example = "WEATHER_NOT_FOUND")
    private String code;
    
    @Schema(description = "Error message", example = "Weather data not found")
    private String message;
    
    @Schema(description = "HTTP status code", example = "404")
    private int status;
    
    @Schema(description = "Request path", example = "/api/v1/weather/123")
    private String path;
    
    @Schema(description = "Timestamp when error occurred")
    private LocalDateTime timestamp;
    
    @Schema(description = "Validation errors")
    private List<ValidationError> validationErrors;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Validation error details")
    public static class ValidationError {
        
        @Schema(description = "Field name", example = "temperature")
        private String field;
        
        @Schema(description = "Rejected value", example = "150")
        private Object rejectedValue;
        
        @Schema(description = "Error message", example = "Temperature must be less than 100°C")
        private String message;
    }
}
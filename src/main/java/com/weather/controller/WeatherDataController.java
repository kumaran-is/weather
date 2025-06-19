package com.weather.controller;

import com.weather.dto.ErrorResponse;
import com.weather.dto.PageResponse;
import com.weather.dto.WeatherDataRequest;
import com.weather.dto.WeatherDataResponse;
import com.weather.service.WeatherDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
@Tag(name = "Weather Data", description = "Weather data management API")
public class WeatherDataController {
    
    private final WeatherDataService weatherDataService;
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create weather data", description = "Create a new weather data record")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Weather data created successfully",
                    content = @Content(schema = @Schema(implementation = WeatherDataResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Mono<WeatherDataResponse> createWeatherData(
            @Valid @RequestBody WeatherDataRequest request) {
        log.info("Creating weather data for city: {}", request.city());
        return weatherDataService.createWeatherData(request);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get weather data by ID", description = "Retrieve weather data by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Weather data found",
                    content = @Content(schema = @Schema(implementation = WeatherDataResponse.class))),
            @ApiResponse(responseCode = "404", description = "Weather data not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Mono<WeatherDataResponse> getWeatherDataById(
            @Parameter(description = "Weather data ID", required = true)
            @PathVariable Long id) {
        log.info("Fetching weather data with id: {}", id);
        return weatherDataService.getWeatherDataById(id);
    }
    
    @GetMapping("/city/{city}")
    @Operation(summary = "Get weather data by city", description = "Retrieve paginated weather data for a specific city")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Weather data retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Mono<PageResponse<WeatherDataResponse>> getWeatherDataByCity(
            @Parameter(description = "City name", required = true)
            @PathVariable String city,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Fetching weather data for city: {}, page: {}, size: {}", city, page, size);
        return weatherDataService.getWeatherDataByCity(city, page, size);
    }
    
    @GetMapping("/city/{city}/latest")
    @Operation(summary = "Get latest weather data by city", description = "Retrieve the most recent weather data for a specific city")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Latest weather data found",
                    content = @Content(schema = @Schema(implementation = WeatherDataResponse.class))),
            @ApiResponse(responseCode = "404", description = "No weather data found for the city",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Mono<WeatherDataResponse> getLatestWeatherDataByCity(
            @Parameter(description = "City name", required = true)
            @PathVariable String city) {
        log.info("Fetching latest weather data for city: {}", city);
        return weatherDataService.getLatestWeatherDataByCity(city);
    }
    
    @GetMapping("/range")
    @Operation(summary = "Get weather data by date range", description = "Retrieve paginated weather data within a specific date range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Weather data retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date range or parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Mono<PageResponse<WeatherDataResponse>> getWeatherDataByDateRange(
            @Parameter(description = "Start date and time", required = true, example = "2024-01-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "End date and time", required = true, example = "2024-01-31T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Fetching weather data for date range: {} to {}, page: {}, size: {}", start, end, page, size);
        return weatherDataService.getWeatherDataByDateRange(start, end, page, size);
    }
    
    @GetMapping("/city/{city}/range")
    @Operation(summary = "Get weather data by city and date range", description = "Retrieve paginated weather data for a specific city within a date range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Weather data retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date range or parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Mono<PageResponse<WeatherDataResponse>> getWeatherDataByCityAndDateRange(
            @Parameter(description = "City name", required = true)
            @PathVariable String city,
            @Parameter(description = "Start date and time", required = true, example = "2024-01-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "End date and time", required = true, example = "2024-01-31T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Fetching weather data for city: {} and date range: {} to {}, page: {}, size: {}", 
                city, start, end, page, size);
        return weatherDataService.getWeatherDataByCityAndDateRange(city, start, end, page, size);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update weather data", description = "Update an existing weather data record")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Weather data updated successfully",
                    content = @Content(schema = @Schema(implementation = WeatherDataResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Weather data not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Mono<WeatherDataResponse> updateWeatherData(
            @Parameter(description = "Weather data ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody WeatherDataRequest request) {
        log.info("Updating weather data with id: {}", id);
        return weatherDataService.updateWeatherData(id, request);
    }
    
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete weather data", description = "Delete a weather data record")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Weather data deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Weather data not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Mono<Void> deleteWeatherData(
            @Parameter(description = "Weather data ID", required = true)
            @PathVariable Long id) {
        log.info("Deleting weather data with id: {}", id);
        return weatherDataService.deleteWeatherData(id);
    }
    
    @GetMapping(value = "/cities", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all cities", description = "Retrieve all cities that have weather data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cities retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Flux<String> getAllCities() {
        log.info("Fetching all cities");
        return weatherDataService.getAllCities();
    }
}
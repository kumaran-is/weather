package com.weather.controller;

import com.weather.dto.ErrorResponse;
import com.weather.dto.PageResponse;
import com.weather.dto.WeatherDataRequest;
import com.weather.dto.WeatherDataResponse;
import com.weather.service.WeatherDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
@Tag(name = "Weather Data", description = "Weather data management API")
public class WeatherDataController {
    
    private static final Logger log = LogManager.getLogger(WeatherDataController.class);
    
    private final WeatherDataService weatherDataService;
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create weather data", 
            description = "Create a new weather data record with comprehensive validation and duplicate detection"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201", 
                    description = "Weather data created successfully",
                    content = @Content(
                            schema = @Schema(implementation = WeatherDataResponse.class),
                            examples = @ExampleObject(
                                    name = "Successful Creation",
                                    summary = "Successfully created weather data",
                                    value = """
                                    {
                                        "id": 1,
                                        "city": "New York",
                                        "country": "USA",
                                        "temperature": 22.5,
                                        "humidity": 65,
                                        "pressure": 1013.25,
                                        "windSpeed": 5.2,
                                        "windDirection": "NW",
                                        "weatherCondition": "Clear",
                                        "description": "Clear skies with light winds",
                                        "recordedAt": "2024-01-15T10:00:00",
                                        "createdAt": "2024-01-15T10:30:00",
                                        "updatedAt": "2024-01-15T10:30:00"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400", 
                    description = "Invalid request data",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Validation Error",
                                            summary = "Request validation failed",
                                            value = """
                                            {
                                                "code": "VALIDATION_ERROR",
                                                "message": "Validation failed",
                                                "status": 400,
                                                "path": "/api/v1/weather",
                                                "timestamp": "2024-01-15T10:30:00",
                                                "validationErrors": [
                                                    {
                                                        "field": "city",
                                                        "rejectedValue": "",
                                                        "message": "City is required"
                                                    },
                                                    {
                                                        "field": "temperature",
                                                        "rejectedValue": 150,
                                                        "message": "Temperature must be less than 100°C"
                                                    }
                                                ]
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "Future Date Error",
                                            summary = "Recorded time in future",
                                            value = """
                                            {
                                                "code": "WEATHER_VALIDATION_ERROR",
                                                "message": "Recorded time cannot be in the future",
                                                "status": 400,
                                                "path": "/api/v1/weather",
                                                "timestamp": "2024-01-15T10:30:00",
                                                "validationErrors": null
                                            }
                                            """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Server Error",
                                    summary = "Internal server error occurred",
                                    value = """
                                    {
                                        "code": "INTERNAL_SERVER_ERROR",
                                        "message": "An unexpected error occurred",
                                        "status": 500,
                                        "path": "/api/v1/weather",
                                        "timestamp": "2024-01-15T10:30:00",
                                        "validationErrors": null
                                    }
                                    """
                            )
                    )
            )
    })
    public Mono<WeatherDataResponse> createWeatherData(
            @Parameter(
                    description = "Weather data to create",
                    required = true,
                    schema = @Schema(implementation = WeatherDataRequest.class),
                    examples = {
                            @ExampleObject(
                                    name = "Clear Weather Example",
                                    summary = "Clear weather in New York",
                                    value = """
                                    {
                                        "city": "New York",
                                        "country": "USA",
                                        "temperature": 22.5,
                                        "humidity": 65,
                                        "pressure": 1013.25,
                                        "windSpeed": 5.2,
                                        "windDirection": "NW",
                                        "weatherCondition": "Clear",
                                        "description": "Clear skies with light winds",
                                        "recordedAt": "2024-01-15T10:00:00"
                                    }
                                    """
                            ),
                            @ExampleObject(
                                    name = "Rainy Weather Example",
                                    summary = "Rainy weather in London",
                                    value = """
                                    {
                                        "city": "London",
                                        "country": "UK",
                                        "temperature": 12.0,
                                        "humidity": 85,
                                        "pressure": 998.5,
                                        "windSpeed": 15.5,
                                        "windDirection": "SW",
                                        "weatherCondition": "Rainy",
                                        "description": "Heavy rain with strong winds",
                                        "recordedAt": "2024-01-15T14:30:00"
                                    }
                                    """
                            )
                    }
            )
            @Valid @RequestBody WeatherDataRequest request) {
        log.info("Creating weather data for city: {}", request.city());
        return weatherDataService.createWeatherData(request);
    }
    
    @GetMapping("/{id}")
    @Operation(
            summary = "Get weather data by ID", 
            description = "Retrieve weather data by its unique identifier with detailed response information"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Weather data found",
                    content = @Content(
                            schema = @Schema(implementation = WeatherDataResponse.class),
                            examples = @ExampleObject(
                                    name = "Found Weather Data",
                                    summary = "Successfully retrieved weather data",
                                    value = """
                                    {
                                        "id": 1,
                                        "city": "Tokyo",
                                        "country": "Japan",
                                        "temperature": 18.5,
                                        "humidity": 70,
                                        "pressure": 1020.0,
                                        "windSpeed": 8.0,
                                        "windDirection": "E",
                                        "weatherCondition": "Cloudy",
                                        "description": "Partly cloudy with gentle breeze",
                                        "recordedAt": "2024-01-15T08:00:00",
                                        "createdAt": "2024-01-15T08:15:00",
                                        "updatedAt": "2024-01-15T08:15:00"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404", 
                    description = "Weather data not found",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Not Found Error",
                                    summary = "Weather data not found for given ID",
                                    value = """
                                    {
                                        "code": "WEATHER_NOT_FOUND",
                                        "message": "Weather data not found with id: 999",
                                        "status": 404,
                                        "path": "/api/v1/weather/999",
                                        "timestamp": "2024-01-15T10:30:00",
                                        "validationErrors": null
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Database Error",
                                    summary = "Database connection error",
                                    value = """
                                    {
                                        "code": "DATA_ACCESS_ERROR",
                                        "message": "Database operation failed",
                                        "status": 503,
                                        "path": "/api/v1/weather/1",
                                        "timestamp": "2024-01-15T10:30:00",
                                        "validationErrors": null
                                    }
                                    """
                            )
                    )
            )
    })
    public Mono<WeatherDataResponse> getWeatherDataById(
            @Parameter(
                    description = "Unique identifier of the weather data record", 
                    required = true,
                    example = "1",
                    schema = @Schema(type = "integer", format = "int64", minimum = "1")
            )
            @PathVariable Long id) {
        log.info("Fetching weather data with id: {}", id);
        return weatherDataService.getWeatherDataById(id);
    }
    
    @GetMapping("/city/{city}")
    @Operation(
            summary = "Get weather data by city", 
            description = "Retrieve paginated weather data for a specific city with sorting and filtering capabilities"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Weather data retrieved successfully",
                    content = @Content(
                            schema = @Schema(implementation = PageResponse.class),
                            examples = @ExampleObject(
                                    name = "Paginated Weather Data",
                                    summary = "Successfully retrieved paginated weather data",
                                    value = """
                                    {
                                        "content": [
                                            {
                                                "id": 1,
                                                "city": "Paris",
                                                "country": "France",
                                                "temperature": 15.0,
                                                "humidity": 75,
                                                "pressure": 1015.0,
                                                "windSpeed": 10.0,
                                                "windDirection": "W",
                                                "weatherCondition": "Cloudy",
                                                "description": "Overcast with light winds",
                                                "recordedAt": "2024-01-15T12:00:00",
                                                "createdAt": "2024-01-15T12:15:00",
                                                "updatedAt": "2024-01-15T12:15:00"
                                            },
                                            {
                                                "id": 2,
                                                "city": "Paris",
                                                "country": "France", 
                                                "temperature": 16.5,
                                                "humidity": 70,
                                                "pressure": 1018.0,
                                                "windSpeed": 8.5,
                                                "windDirection": "SW",
                                                "weatherCondition": "Sunny",
                                                "description": "Clear sunny day",
                                                "recordedAt": "2024-01-15T09:00:00",
                                                "createdAt": "2024-01-15T09:15:00",
                                                "updatedAt": "2024-01-15T09:15:00"
                                            }
                                        ],
                                        "page": 0,
                                        "size": 20,
                                        "totalElements": 2,
                                        "totalPages": 1,
                                        "first": true,
                                        "last": true,
                                        "numberOfElements": 2,
                                        "empty": false
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400", 
                    description = "Invalid request parameters",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Invalid Parameters",
                                    summary = "Invalid pagination parameters",
                                    value = """
                                    {
                                        "code": "VALIDATION_ERROR",
                                        "message": "Validation failed",
                                        "status": 400,
                                        "path": "/api/v1/weather/city/London",
                                        "timestamp": "2024-01-15T10:30:00",
                                        "validationErrors": [
                                            {
                                                "field": "page",
                                                "rejectedValue": -1,
                                                "message": "Page number must be greater than or equal to 0"
                                            },
                                            {
                                                "field": "size",
                                                "rejectedValue": 150,
                                                "message": "Page size must be less than or equal to 100"
                                            }
                                        ]
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public Mono<PageResponse<WeatherDataResponse>> getWeatherDataByCity(
            @Parameter(
                    description = "Name of the city to search for weather data", 
                    required = true,
                    example = "London",
                    schema = @Schema(type = "string", minLength = 1, maxLength = 100)
            )
            @PathVariable String city,
            @Parameter(
                    description = "Page number for pagination (0-based indexing)", 
                    example = "0",
                    schema = @Schema(type = "integer", minimum = "0", defaultValue = "0")
            )
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(
                    description = "Number of items per page (maximum 100)", 
                    example = "20",
                    schema = @Schema(type = "integer", minimum = "1", maximum = "100", defaultValue = "20")
            )
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
            @Parameter(description = "City name", required = true, example = "London")
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
            @Parameter(description = "City name", required = true, example = "London")
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
    @Operation(
            summary = "Get all cities", 
            description = "Retrieve all unique cities that have weather data, ordered alphabetically using Java 21 SequencedCollection"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Cities retrieved successfully",
                    content = @Content(
                            schema = @Schema(
                                    type = "array",
                                    implementation = String.class,
                                    example = "[\"Berlin\", \"London\", \"New York\", \"Paris\", \"Tokyo\"]"
                            ),
                            examples = @ExampleObject(
                                    name = "Cities List",
                                    summary = "List of all cities with weather data",
                                    value = """
                                    [
                                        "Berlin",
                                        "London", 
                                        "New York",
                                        "Paris",
                                        "Sydney",
                                        "Tokyo"
                                    ]
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Service Unavailable",
                                    summary = "Database service temporarily unavailable",
                                    value = """
                                    {
                                        "code": "DATA_ACCESS_ERROR",
                                        "message": "Database operation failed",
                                        "status": 503,
                                        "path": "/api/v1/weather/cities",
                                        "timestamp": "2024-01-15T10:30:00",
                                        "validationErrors": null
                                    }
                                    """
                            )
                    )
            )
    })
    public Mono<List<String>> getAllCities() {
        log.info("Fetching all cities");
        return weatherDataService.getAllCities();
    }
}
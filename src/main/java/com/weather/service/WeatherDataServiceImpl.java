package com.weather.service;

import com.weather.dto.PageResponse;
import com.weather.dto.WeatherDataRequest;
import com.weather.dto.WeatherDataResponse;
import com.weather.exception.WeatherNotFoundException;
import com.weather.exception.WeatherServiceException;
import com.weather.exception.WeatherValidationException;
import com.weather.mapper.WeatherDataMapper;
import com.weather.repository.WeatherDataRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedCollection;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherDataServiceImpl implements WeatherDataService {
    
    private final WeatherDataRepository repository;
    private final WeatherDataMapper mapper;
    
    @Override
    @CircuitBreaker(name = "weather-service", fallbackMethod = "createWeatherDataFallback")
    @Retry(name = "weather-service")
    @TimeLimiter(name = "weather-service")
    @Transactional
    public Mono<WeatherDataResponse> createWeatherData(WeatherDataRequest request) {
        return validateRequest(request)
                .then(Mono.fromCallable(() -> mapper.toEntity(request)))
                .doOnNext(entity -> 
                    // Context automatically flows here - no manual extraction needed
                    log.debug("Creating weather data for city: {} (context flows automatically)", entity.getCity())
                )
                .flatMap(repository::save)
                .map(mapper::toResponse)
                .doOnSuccess(response -> 
                    // Context with correlation ID is automatically available
                    log.info("Created weather data with id: {} (correlation ID in context)", response.id())
                )
                .onErrorMap(this::mapToServiceException);
    }
    
    @Override
    @CircuitBreaker(name = "weather-service", fallbackMethod = "getWeatherDataByIdFallback")
    @Retry(name = "weather-service")
    @TimeLimiter(name = "weather-service")
    public Mono<WeatherDataResponse> getWeatherDataById(Long id) {
        log.debug("Fetching weather data by id: {}", id);
        
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new WeatherNotFoundException(id)))
                .map(mapper::toResponse)
                .doOnSuccess(response -> log.debug("Found weather data: {}", response.id()))
                .onErrorMap(this::mapToServiceException);
    }
    
    @Override
    @CircuitBreaker(name = "weather-service", fallbackMethod = "getWeatherDataByCityFallback")
    @Retry(name = "weather-service")
    @TimeLimiter(name = "weather-service")
    public Mono<PageResponse<WeatherDataResponse>> getWeatherDataByCity(String city, int page, int size) {
        log.debug("Fetching weather data for city: {}, page: {}, size: {}", city, page, size);
        
        long offset = (long) page * size;
        
        return Mono.zip(
                repository.findByCityOrderByRecordedAtDesc(city, size, offset)
                        .map(mapper::toResponse)
                        .collectList(),
                repository.countByCity(city)
        ).map(tuple -> buildPageResponse(tuple.getT1(), page, size, tuple.getT2()))
                .doOnSuccess(response -> log.debug("Found {} weather records for city: {}", 
                        response.numberOfElements(), city))
                .onErrorMap(this::mapToServiceException);
    }
    
    @Override
    @CircuitBreaker(name = "weather-service", fallbackMethod = "getLatestWeatherDataByCityFallback")
    @Retry(name = "weather-service")
    @TimeLimiter(name = "weather-service")
    public Mono<WeatherDataResponse> getLatestWeatherDataByCity(String city) {
        log.debug("Fetching latest weather data for city: {}", city);
        
        return repository.findLatestByCityOrderByRecordedAtDesc(city)
                .switchIfEmpty(Mono.error(new WeatherNotFoundException(city)))
                .map(mapper::toResponse)
                .doOnSuccess(response -> log.debug("Found latest weather data for city: {}", city))
                .onErrorMap(this::mapToServiceException);
    }
    
    @Override
    @CircuitBreaker(name = "weather-service", fallbackMethod = "getWeatherDataByDateRangeFallback")
    @Retry(name = "weather-service")
    @TimeLimiter(name = "weather-service")
    public Mono<PageResponse<WeatherDataResponse>> getWeatherDataByDateRange(LocalDateTime start, LocalDateTime end, int page, int size) {
        log.debug("Fetching weather data for date range: {} to {}, page: {}, size: {}", start, end, page, size);
        
        return validateDateRange(start, end)
                .then(Mono.defer(() -> {
                    long offset = (long) page * size;
                    return Mono.zip(
                            repository.findByRecordedAtBetweenOrderByRecordedAtDesc(start, end, size, offset)
                                    .map(mapper::toResponse)
                                    .collectList(),
                            repository.countByRecordedAtBetween(start, end)
                    );
                }))
                .map(tuple -> buildPageResponse(tuple.getT1(), page, size, tuple.getT2()))
                .doOnSuccess(response -> log.debug("Found {} weather records for date range", 
                        response.numberOfElements()))
                .onErrorMap(this::mapToServiceException);
    }
    
    @Override
    @CircuitBreaker(name = "weather-service", fallbackMethod = "getWeatherDataByCityAndDateRangeFallback")
    @Retry(name = "weather-service")
    @TimeLimiter(name = "weather-service")
    public Mono<PageResponse<WeatherDataResponse>> getWeatherDataByCityAndDateRange(String city, LocalDateTime start, LocalDateTime end, int page, int size) {
        log.debug("Fetching weather data for city: {} and date range: {} to {}, page: {}, size: {}", 
                city, start, end, page, size);
        
        return validateDateRange(start, end)
                .then(Mono.defer(() -> {
                    long offset = (long) page * size;
                    return Mono.zip(
                            repository.findByCityAndRecordedAtBetweenOrderByRecordedAtDesc(city, start, end, size, offset)
                                    .map(mapper::toResponse)
                                    .collectList(),
                            repository.countByCityAndRecordedAtBetween(city, start, end)
                    );
                }))
                .map(tuple -> buildPageResponse(tuple.getT1(), page, size, tuple.getT2()))
                .doOnSuccess(response -> log.debug("Found {} weather records for city: {} and date range", 
                        response.numberOfElements(), city))
                .onErrorMap(this::mapToServiceException);
    }
    
    @Override
    @CircuitBreaker(name = "weather-service", fallbackMethod = "updateWeatherDataFallback")
    @Retry(name = "weather-service")
    @TimeLimiter(name = "weather-service")
    @Transactional
    public Mono<WeatherDataResponse> updateWeatherData(Long id, WeatherDataRequest request) {
        log.debug("Updating weather data with id: {}", id);
        
        return validateRequest(request)
                .then(repository.findById(id))
                .switchIfEmpty(Mono.error(new WeatherNotFoundException(id)))
                .doOnNext(existing -> mapper.updateEntityFromRequest(request, existing))
                .flatMap(repository::save)
                .map(mapper::toResponse)
                .doOnSuccess(response -> log.info("Updated weather data with id: {}", response.id()))
                .onErrorMap(this::mapToServiceException);
    }
    
    @Override
    @CircuitBreaker(name = "weather-service", fallbackMethod = "deleteWeatherDataFallback")
    @Retry(name = "weather-service")
    @TimeLimiter(name = "weather-service")
    @Transactional
    public Mono<Void> deleteWeatherData(Long id) {
        log.debug("Deleting weather data with id: {}", id);
        
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new WeatherNotFoundException(id)))
                .flatMap(repository::delete)
                .doOnSuccess(unused -> log.info("Deleted weather data with id: {}", id))
                .onErrorMap(this::mapToServiceException);
    }
    
    @Override
    @CircuitBreaker(name = "weather-service", fallbackMethod = "getAllCitiesFallback")
    @Retry(name = "weather-service")
    @TimeLimiter(name = "weather-service")
    public Flux<String> getAllCities() {
        // Cache cities list for 5 minutes to avoid re-subscribing to cold publisher
        // Cities don't change frequently, making this an ideal candidate for caching
        return repository.findDistinctCities()
                .doOnSubscribe(sub -> 
                    // Context propagation happens automatically - correlation ID available
                    log.debug("Subscribed to cities stream - cache will be used if available (context auto-propagated)")
                )
                .collectList()
                .map(this::buildOrderedUniqueCollection)
                .cache(Duration.ofMinutes(5)) // Cache for 5 minutes
                .flatMapMany(orderedCities -> Flux.fromIterable(orderedCities))
                .doOnComplete(() -> 
                    // Context still flows through the entire reactive chain automatically
                    log.debug("Successfully fetched all cities from cache or database (context preserved)")
                )
                .onErrorMap(this::mapToServiceException);
    }
    
    private Mono<Void> validateRequest(WeatherDataRequest request) {
        if (request.recordedAt().isAfter(LocalDateTime.now())) {
            return Mono.error(new WeatherValidationException("Recorded time cannot be in the future"));
        }
        return validateWeatherCondition(request.weatherCondition());
    }
    
    private Mono<Void> validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end)) {
            return Mono.error(new WeatherValidationException("Start date cannot be after end date"));
        }
        return Mono.empty();
    }
    
    // Java 21 Switch Expression for handling sort direction preferences
    private String buildOrderByClause(String sortField, String sortDirection) {
        String field = switch (sortField) {
            case "temperature" -> "temperature";
            case "humidity" -> "humidity";
            case "pressure" -> "pressure";
            case "windSpeed" -> "wind_speed";
            case "city" -> "city";
            case "recordedAt" -> "recorded_at";
            default -> "recorded_at"; // Default sorting
        };
        
        String direction = switch (sortDirection.toUpperCase()) {
            case "ASC", "ASCENDING" -> "ASC";
            case "DESC", "DESCENDING" -> "DESC";
            default -> "DESC"; // Default to descending
        };
        
        return field + " " + direction;
    }
    
    // Enhanced validation with switch expressions
    private Mono<Void> validateWeatherCondition(String condition) {
        if (condition == null || condition.isBlank()) {
            return Mono.empty();
        }
        
        return switch (condition.toLowerCase().trim()) {
            case "clear", "sunny", "cloudy", "overcast", "rainy", "stormy", 
                 "snowy", "foggy", "windy", "humid", "dry" -> Mono.empty();
            default -> Mono.error(new WeatherValidationException(
                    "Invalid weather condition: " + condition + 
                    ". Allowed values: clear, sunny, cloudy, overcast, rainy, stormy, snowy, foggy, windy, humid, dry"));
        };
    }
    
    // Enhanced with Java 21 SequencedCollection for predictable ordering
    private <T> PageResponse<T> buildPageResponse(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        // Use SequencedCollection to ensure predictable ordering
        SequencedCollection<T> orderedContent = new ArrayList<>(content);
        List<T> finalContent = new ArrayList<>(orderedContent);
        
        return new PageResponse<>(
                finalContent,
                page,
                size,
                totalElements,
                totalPages,
                page == 0,
                page >= totalPages - 1,
                finalContent.size(),
                finalContent.isEmpty()
        );
    }
    
    // Utility method for handling unique ordered cities using SequencedCollection
    private SequencedCollection<String> buildOrderedUniqueCollection(List<String> cities) {
        // LinkedHashSet maintains insertion order while ensuring uniqueness
        SequencedCollection<String> orderedCities = new LinkedHashSet<>();
        orderedCities.addAll(cities);
        return orderedCities;
    }
    
    private Throwable mapToServiceException(Throwable throwable) {
        // Java 21 Switch Expression for cleaner exception mapping
        return switch (throwable) {
            case WeatherNotFoundException wnfEx -> {
                log.debug("Weather data not found: {}", wnfEx.getMessage());
                yield wnfEx;
            }
            case WeatherValidationException wvEx -> {
                log.debug("Validation error: {}", wvEx.getMessage());
                yield wvEx;
            }
            case org.springframework.dao.DataAccessException daEx -> {
                log.error("Database access error in weather service", daEx);
                yield new WeatherServiceException("Database operation failed", daEx);
            }
            case java.util.concurrent.TimeoutException tEx -> {
                log.error("Timeout error in weather service", tEx);
                yield new WeatherServiceException("Operation timed out", tEx);
            }
            case IllegalArgumentException iaEx -> {
                log.error("Invalid argument in weather service", iaEx);
                yield new WeatherValidationException("Invalid input: " + iaEx.getMessage());
            }
            default -> {
                log.error("Unexpected error in weather service", throwable);
                yield new WeatherServiceException("An error occurred while processing weather data", throwable);
            }
        };
    }
    
    // Fallback methods for circuit breaker
    public Mono<WeatherDataResponse> createWeatherDataFallback(WeatherDataRequest request, Exception ex) {
        log.error("Circuit breaker activated for createWeatherData", ex);
        return Mono.error(new WeatherServiceException("Weather service is temporarily unavailable"));
    }
    
    public Mono<WeatherDataResponse> getWeatherDataByIdFallback(Long id, Exception ex) {
        log.error("Circuit breaker activated for getWeatherDataById", ex);
        return Mono.error(new WeatherServiceException("Weather service is temporarily unavailable"));
    }
    
    public Mono<PageResponse<WeatherDataResponse>> getWeatherDataByCityFallback(String city, int page, int size, Exception ex) {
        log.error("Circuit breaker activated for getWeatherDataByCity", ex);
        return Mono.error(new WeatherServiceException("Weather service is temporarily unavailable"));
    }
    
    public Mono<WeatherDataResponse> getLatestWeatherDataByCityFallback(String city, Exception ex) {
        log.error("Circuit breaker activated for getLatestWeatherDataByCity", ex);
        return Mono.error(new WeatherServiceException("Weather service is temporarily unavailable"));
    }
    
    public Mono<PageResponse<WeatherDataResponse>> getWeatherDataByDateRangeFallback(LocalDateTime start, LocalDateTime end, int page, int size, Exception ex) {
        log.error("Circuit breaker activated for getWeatherDataByDateRange", ex);
        return Mono.error(new WeatherServiceException("Weather service is temporarily unavailable"));
    }
    
    public Mono<PageResponse<WeatherDataResponse>> getWeatherDataByCityAndDateRangeFallback(String city, LocalDateTime start, LocalDateTime end, int page, int size, Exception ex) {
        log.error("Circuit breaker activated for getWeatherDataByCityAndDateRange", ex);
        return Mono.error(new WeatherServiceException("Weather service is temporarily unavailable"));
    }
    
    public Mono<WeatherDataResponse> updateWeatherDataFallback(Long id, WeatherDataRequest request, Exception ex) {
        log.error("Circuit breaker activated for updateWeatherData", ex);
        return Mono.error(new WeatherServiceException("Weather service is temporarily unavailable"));
    }
    
    public Mono<Void> deleteWeatherDataFallback(Long id, Exception ex) {
        log.error("Circuit breaker activated for deleteWeatherData", ex);
        return Mono.error(new WeatherServiceException("Weather service is temporarily unavailable"));
    }
    
    public Flux<String> getAllCitiesFallback(Exception ex) {
        log.error("Circuit breaker activated for getAllCities", ex);
        return Flux.error(new WeatherServiceException("Weather service is temporarily unavailable"));
    }
}
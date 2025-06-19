package com.weather.service.impl;

import com.weather.dto.PageResponse;
import com.weather.dto.WeatherDataRequest;
import com.weather.dto.WeatherDataResponse;
import com.weather.entity.WeatherData;
import com.weather.exception.WeatherNotFoundException;
import com.weather.exception.WeatherServiceException;
import com.weather.exception.WeatherValidationException;
import com.weather.mapper.WeatherDataMapper;
import com.weather.repository.WeatherDataRepository;
import com.weather.service.WeatherDataService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

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
        log.debug("Creating weather data for city: {}", request.city());
        
        return validateRequest(request)
                .then(Mono.fromCallable(() -> mapper.toEntity(request)))
                .flatMap(repository::save)
                .map(mapper::toResponse)
                .doOnSuccess(response -> log.info("Created weather data with id: {}", response.id()))
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
        log.debug("Fetching all cities");
        
        return repository.findDistinctCities()
                .doOnComplete(() -> log.debug("Successfully fetched all cities"))
                .onErrorMap(this::mapToServiceException);
    }
    
    private Mono<Void> validateRequest(WeatherDataRequest request) {
        if (request.recordedAt().isAfter(LocalDateTime.now())) {
            return Mono.error(new WeatherValidationException("Recorded time cannot be in the future"));
        }
        return Mono.empty();
    }
    
    private Mono<Void> validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end)) {
            return Mono.error(new WeatherValidationException("Start date cannot be after end date"));
        }
        return Mono.empty();
    }
    
    private <T> PageResponse<T> buildPageResponse(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        return new PageResponse<>(
                content,
                page,
                size,
                totalElements,
                totalPages,
                page == 0,
                page >= totalPages - 1,
                content.size(),
                content.isEmpty()
        );
    }
    
    private Throwable mapToServiceException(Throwable throwable) {
        if (throwable instanceof WeatherNotFoundException || 
            throwable instanceof WeatherValidationException) {
            return throwable;
        }
        log.error("Unexpected error in weather service", throwable);
        return new WeatherServiceException("An error occurred while processing weather data", throwable);
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
package com.weather.service;

import com.weather.dto.PageResponse;
import com.weather.dto.WeatherDataRequest;
import com.weather.dto.WeatherDataResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface WeatherDataService {
    
    Mono<WeatherDataResponse> createWeatherData(WeatherDataRequest request);
    
    Mono<WeatherDataResponse> getWeatherDataById(Long id);
    
    Mono<PageResponse<WeatherDataResponse>> getWeatherDataByCity(String city, int page, int size);
    
    Mono<WeatherDataResponse> getLatestWeatherDataByCity(String city);
    
    Mono<PageResponse<WeatherDataResponse>> getWeatherDataByDateRange(LocalDateTime start, LocalDateTime end, int page, int size);
    
    Mono<PageResponse<WeatherDataResponse>> getWeatherDataByCityAndDateRange(String city, LocalDateTime start, LocalDateTime end, int page, int size);
    
    Mono<WeatherDataResponse> updateWeatherData(Long id, WeatherDataRequest request);
    
    Mono<Void> deleteWeatherData(Long id);
    
    Flux<String> getAllCities();
}
package com.weather.exception;

import org.springframework.http.HttpStatus;

public class WeatherNotFoundException extends BaseException {
    
    private static final String CODE = "WEATHER_NOT_FOUND";
    
    public WeatherNotFoundException(Long id) {
        super(CODE, "Weather data not found with id: " + id, HttpStatus.NOT_FOUND);
    }
    
    public WeatherNotFoundException(String city) {
        super(CODE, "Weather data not found for city: " + city, HttpStatus.NOT_FOUND);
    }
    
    public WeatherNotFoundException(String message, Throwable cause) {
        super(CODE, message, HttpStatus.NOT_FOUND, cause);
    }
}
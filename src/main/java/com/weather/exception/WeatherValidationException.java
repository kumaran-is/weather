package com.weather.exception;

import org.springframework.http.HttpStatus;

public class WeatherValidationException extends BaseException {
    
    private static final String CODE = "WEATHER_VALIDATION_ERROR";
    
    public WeatherValidationException(String message) {
        super(CODE, message, HttpStatus.BAD_REQUEST);
    }
    
    public WeatherValidationException(String message, Throwable cause) {
        super(CODE, message, HttpStatus.BAD_REQUEST, cause);
    }
}
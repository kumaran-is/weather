package com.weather.exception;

import org.springframework.http.HttpStatus;

public class WeatherServiceException extends BaseException {
    
    private static final String CODE = "WEATHER_SERVICE_ERROR";
    
    public WeatherServiceException(String message) {
        super(CODE, message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    public WeatherServiceException(String message, Throwable cause) {
        super(CODE, message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
package com.weather.exception;

import com.weather.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {
    
    private final ObjectMapper objectMapper;
    
    public GlobalErrorWebExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        log.error("Error occurred: ", ex);
        
        ErrorResponse errorResponse = buildErrorResponse(exchange, ex);
        
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(errorResponse.status()));
        exchange.getResponse().getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        
        try {
            String responseBody = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(responseBody.getBytes());
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("Error writing response", e);
            return exchange.getResponse().setComplete();
        }
    }
    
    private ErrorResponse buildErrorResponse(ServerWebExchange exchange, Throwable ex) {
        String path = exchange.getRequest().getPath().value();
        LocalDateTime timestamp = LocalDateTime.now();
        
        // Java 21 Pattern Matching for enhanced error handling
        return switch (ex) {
            case BaseException baseEx -> new ErrorResponse(
                    baseEx.getCode(),
                    baseEx.getMessage(),
                    baseEx.getHttpStatus().value(),
                    path,
                    timestamp,
                    null
            );
            
            case WebExchangeBindException bindEx -> {
                List<ErrorResponse.ValidationError> validationErrors = bindEx.getFieldErrors()
                        .stream()
                        .map(fieldError -> new ErrorResponse.ValidationError(
                                fieldError.getField(),
                                fieldError.getRejectedValue(),
                                fieldError.getDefaultMessage()
                        ))
                        .collect(Collectors.toList());
                
                yield new ErrorResponse(
                        "VALIDATION_ERROR",
                        "Validation failed",
                        HttpStatus.BAD_REQUEST.value(),
                        path,
                        timestamp,
                        validationErrors
                );
            }
            
            // Pattern matching for specific exception types with enhanced error codes
            case IllegalArgumentException illegalArgEx -> new ErrorResponse(
                    "INVALID_ARGUMENT",
                    "Invalid argument provided: " + illegalArgEx.getMessage(),
                    HttpStatus.BAD_REQUEST.value(),
                    path,
                    timestamp,
                    null
            );
            
            case java.util.concurrent.TimeoutException timeoutEx -> new ErrorResponse(
                    "REQUEST_TIMEOUT",
                    "Request processing timed out",
                    HttpStatus.REQUEST_TIMEOUT.value(),
                    path,
                    timestamp,
                    null
            );
            
            case org.springframework.dao.DataAccessException dataEx -> new ErrorResponse(
                    "DATA_ACCESS_ERROR",
                    "Database operation failed",
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    path,
                    timestamp,
                    null
            );
            
            // Default case for unhandled exceptions
            default -> new ErrorResponse(
                    "INTERNAL_SERVER_ERROR",
                    "An unexpected error occurred",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    path,
                    timestamp,
                    null
            );
        };
    }
}
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
        
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(errorResponse.getStatus()));
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
        
        if (ex instanceof BaseException baseEx) {
            return ErrorResponse.builder()
                    .code(baseEx.getCode())
                    .message(baseEx.getMessage())
                    .status(baseEx.getHttpStatus().value())
                    .path(path)
                    .timestamp(timestamp)
                    .build();
        }
        
        if (ex instanceof WebExchangeBindException bindEx) {
            List<ErrorResponse.ValidationError> validationErrors = bindEx.getFieldErrors()
                    .stream()
                    .map(fieldError -> ErrorResponse.ValidationError.builder()
                            .field(fieldError.getField())
                            .rejectedValue(fieldError.getRejectedValue())
                            .message(fieldError.getDefaultMessage())
                            .build())
                    .collect(Collectors.toList());
            
            return ErrorResponse.builder()
                    .code("VALIDATION_ERROR")
                    .message("Validation failed")
                    .status(HttpStatus.BAD_REQUEST.value())
                    .path(path)
                    .timestamp(timestamp)
                    .validationErrors(validationErrors)
                    .build();
        }
        
        // Default error response
        return ErrorResponse.builder()
                .code("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .path(path)
                .timestamp(timestamp)
                .build();
    }
}
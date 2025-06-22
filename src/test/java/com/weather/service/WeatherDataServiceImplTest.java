package com.weather.service;

import com.weather.dto.WeatherDataRequest;
import com.weather.dto.WeatherDataResponse;
import com.weather.entity.WeatherData;
import com.weather.exception.WeatherNotFoundException;
import com.weather.mapper.WeatherDataMapper;
import com.weather.repository.WeatherDataRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherDataServiceImplTest {

    @Mock
    private WeatherDataRepository repository;

    @Mock
    private WeatherDataMapper mapper;

    @InjectMocks
    private WeatherDataServiceImpl weatherDataService;

    private WeatherDataRequest testRequest;
    private WeatherData testEntity;
    private WeatherDataResponse testResponse;

    @BeforeEach
    void setUp() {
        testRequest = new WeatherDataRequest(
                "Test City",
                "Test Country",
                new BigDecimal("25.5"),
                60,
                new BigDecimal("1013.25"),
                new BigDecimal("5.2"),
                "NW",
                "Clear",
                "Clear skies",
                LocalDateTime.now().minusHours(1)
        );

        testEntity = WeatherData.builder()
                .id(1L)
                .city("Test City")
                .country("Test Country")
                .temperature(new BigDecimal("25.5"))
                .humidity(60)
                .pressure(new BigDecimal("1013.25"))
                .windSpeed(new BigDecimal("5.2"))
                .windDirection("NW")
                .weatherCondition("Clear")
                .description("Clear skies")
                .recordedAt(LocalDateTime.now().minusHours(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testResponse = new WeatherDataResponse(
                1L,
                "Test City",
                "Test Country",
                new BigDecimal("25.5"),
                60,
                new BigDecimal("1013.25"),
                new BigDecimal("5.2"),
                "NW",
                "Clear",
                "Clear skies",
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    void createWeatherData_ShouldReturnWeatherDataResponse() {
        when(mapper.toEntity(any(WeatherDataRequest.class))).thenReturn(testEntity);
        when(repository.save(any(WeatherData.class))).thenReturn(Mono.just(testEntity));
        when(mapper.toResponse(any(WeatherData.class))).thenReturn(testResponse);

        StepVerifier.create(weatherDataService.createWeatherData(testRequest))
                .expectNext(testResponse)
                .verifyComplete();
    }

    @Test
    void getWeatherDataById_WhenExists_ShouldReturnWeatherDataResponse() {
        when(repository.findById(anyLong())).thenReturn(Mono.just(testEntity));
        when(mapper.toResponse(any(WeatherData.class))).thenReturn(testResponse);

        StepVerifier.create(weatherDataService.getWeatherDataById(1L))
                .expectNext(testResponse)
                .verifyComplete();
    }

    @Test
    void getWeatherDataById_WhenNotExists_ShouldThrowNotFoundException() {
        when(repository.findById(anyLong())).thenReturn(Mono.empty());

        StepVerifier.create(weatherDataService.getWeatherDataById(1L))
                .expectError(WeatherNotFoundException.class)
                .verify();
    }
}
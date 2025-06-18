package com.weather.controller;

import com.weather.dto.WeatherDataRequest;
import com.weather.dto.WeatherDataResponse;
import com.weather.service.WeatherDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@WebFluxTest(WeatherDataController.class)
class WeatherDataControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private WeatherDataService weatherDataService;

    private WeatherDataRequest testRequest;
    private WeatherDataResponse testResponse;

    @BeforeEach
    void setUp() {
        testRequest = WeatherDataRequest.builder()
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
                .build();

        testResponse = WeatherDataResponse.builder()
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
    }

    @Test
    void createWeatherData_ShouldReturnCreated() {
        when(weatherDataService.createWeatherData(any(WeatherDataRequest.class)))
                .thenReturn(Mono.just(testResponse));

        webTestClient.post()
                .uri("/api/v1/weather")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(WeatherDataResponse.class)
                .value(response -> {
                    assert response.getId().equals(1L);
                    assert response.getCity().equals("Test City");
                });
    }

    @Test
    void getWeatherDataById_ShouldReturnOk() {
        when(weatherDataService.getWeatherDataById(anyLong()))
                .thenReturn(Mono.just(testResponse));

        webTestClient.get()
                .uri("/api/v1/weather/1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(WeatherDataResponse.class)
                .value(response -> {
                    assert response.getId().equals(1L);
                    assert response.getCity().equals("Test City");
                });
    }

    @Test
    void createWeatherData_WithInvalidData_ShouldReturnBadRequest() {
        WeatherDataRequest invalidRequest = WeatherDataRequest.builder()
                .city("") // Invalid: empty city
                .temperature(new BigDecimal("150")) // Invalid: temperature too high
                .recordedAt(LocalDateTime.now().plusHours(1)) // Invalid: future date
                .build();

        webTestClient.post()
                .uri("/api/v1/weather")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
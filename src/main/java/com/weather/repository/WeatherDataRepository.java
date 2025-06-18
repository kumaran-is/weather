package com.weather.repository;

import com.weather.entity.WeatherData;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface WeatherDataRepository extends R2dbcRepository<WeatherData, Long> {
    
    @Query("SELECT * FROM weather_data WHERE city = :city ORDER BY recorded_at DESC LIMIT :size OFFSET :offset")
    Flux<WeatherData> findByCityOrderByRecordedAtDesc(@Param("city") String city, 
                                                      @Param("size") int size, 
                                                      @Param("offset") long offset);
    
    @Query("SELECT COUNT(*) FROM weather_data WHERE city = :city")
    Mono<Long> countByCity(@Param("city") String city);
    
    @Query("SELECT * FROM weather_data WHERE city = :city ORDER BY recorded_at DESC LIMIT 1")
    Mono<WeatherData> findLatestByCityOrderByRecordedAtDesc(@Param("city") String city);
    
    @Query("SELECT * FROM weather_data WHERE recorded_at BETWEEN :start AND :end ORDER BY recorded_at DESC LIMIT :size OFFSET :offset")
    Flux<WeatherData> findByRecordedAtBetweenOrderByRecordedAtDesc(@Param("start") LocalDateTime start,
                                                                  @Param("end") LocalDateTime end,
                                                                  @Param("size") int size,
                                                                  @Param("offset") long offset);
    
    @Query("SELECT COUNT(*) FROM weather_data WHERE recorded_at BETWEEN :start AND :end")
    Mono<Long> countByRecordedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT * FROM weather_data WHERE city = :city AND recorded_at BETWEEN :start AND :end ORDER BY recorded_at DESC LIMIT :size OFFSET :offset")
    Flux<WeatherData> findByCityAndRecordedAtBetweenOrderByRecordedAtDesc(@Param("city") String city,
                                                                         @Param("start") LocalDateTime start,
                                                                         @Param("end") LocalDateTime end,
                                                                         @Param("size") int size,
                                                                         @Param("offset") long offset);
    
    @Query("SELECT COUNT(*) FROM weather_data WHERE city = :city AND recorded_at BETWEEN :start AND :end")
    Mono<Long> countByCityAndRecordedAtBetween(@Param("city") String city,
                                              @Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);
    
    @Query("SELECT DISTINCT city FROM weather_data ORDER BY city")
    Flux<String> findDistinctCities();
}
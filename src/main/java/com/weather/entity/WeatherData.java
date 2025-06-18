package com.weather.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("weather_data")
public class WeatherData {
    
    @Id
    private Long id;
    
    @Column("city")
    private String city;
    
    @Column("country")
    private String country;
    
    @Column("temperature")
    private BigDecimal temperature;
    
    @Column("humidity")
    private Integer humidity;
    
    @Column("pressure")
    private BigDecimal pressure;
    
    @Column("wind_speed")
    private BigDecimal windSpeed;
    
    @Column("wind_direction")
    private String windDirection;
    
    @Column("weather_condition")
    private String weatherCondition;
    
    @Column("description")
    private String description;
    
    @Column("recorded_at")
    private LocalDateTime recordedAt;
    
    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
package com.weather.mapper;

import com.weather.dto.WeatherDataRequest;
import com.weather.dto.WeatherDataResponse;
import com.weather.entity.WeatherData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", 
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface WeatherDataMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    WeatherData toEntity(WeatherDataRequest request);
    
    WeatherDataResponse toResponse(WeatherData entity);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(WeatherDataRequest request, @MappingTarget WeatherData entity);
}
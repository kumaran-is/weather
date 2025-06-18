package com.weather.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated response")
public record PageResponse<T>(
    
    @Schema(description = "Page content")
    List<T> content,
    
    @Schema(description = "Current page number (0-based)", example = "0")
    int page,
    
    @Schema(description = "Page size", example = "20")
    int size,
    
    @Schema(description = "Total number of elements", example = "100")
    long totalElements,
    
    @Schema(description = "Total number of pages", example = "5")
    int totalPages,
    
    @Schema(description = "Is first page", example = "true")
    boolean first,
    
    @Schema(description = "Is last page", example = "false")
    boolean last,
    
    @Schema(description = "Number of elements in current page", example = "20")
    int numberOfElements,
    
    @Schema(description = "Is page empty", example = "false")
    boolean empty
) {}
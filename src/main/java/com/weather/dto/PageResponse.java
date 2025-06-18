package com.weather.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated response")
public class PageResponse<T> {
    
    @Schema(description = "Page content")
    private List<T> content;
    
    @Schema(description = "Current page number (0-based)", example = "0")
    private int page;
    
    @Schema(description = "Page size", example = "20")
    private int size;
    
    @Schema(description = "Total number of elements", example = "100")
    private long totalElements;
    
    @Schema(description = "Total number of pages", example = "5")
    private int totalPages;
    
    @Schema(description = "Is first page", example = "true")
    private boolean first;
    
    @Schema(description = "Is last page", example = "false")
    private boolean last;
    
    @Schema(description = "Number of elements in current page", example = "20")
    private int numberOfElements;
    
    @Schema(description = "Is page empty", example = "false")
    private boolean empty;
}
package com.ash.tracker_service.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ExcelUploadResponseDTO {
    
    private String status;
    private Integer totalStocks;
    private Integer addedStocks;
    private Integer updatedStocks;
    private Integer potentiallySoldStocks;
    private List<SoldStockDetectionDTO> detectedSoldStocks;
    private String message;
}

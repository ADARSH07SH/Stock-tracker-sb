package com.ash.tracker_service.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SoldStockDetectionDTO {
    
    private String stockName;
    private String isin;
    private Integer quantity;
    private Double averageBuyPrice;
    private Double investedValue;
}

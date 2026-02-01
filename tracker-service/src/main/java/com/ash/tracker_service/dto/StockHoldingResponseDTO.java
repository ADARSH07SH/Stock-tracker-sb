package com.ash.tracker_service.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StockHoldingResponseDTO {

    private String stockName;
    private String isin;

    private Integer quantity;
    private Double averageBuyPrice;

    private Double currentPrice;
    private Double currentValue;
    private Double unrealisedPL;
    private Double returnPercentage;
    
    private Boolean priceUnavailable;
}

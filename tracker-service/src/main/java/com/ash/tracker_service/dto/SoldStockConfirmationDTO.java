package com.ash.tracker_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SoldStockConfirmationDTO {
    
    private String stockName;
    private String isin;
    private Integer quantity;
    private Double averageBuyPrice;
    private Double sellPrice;
    private Double investedValue;
}

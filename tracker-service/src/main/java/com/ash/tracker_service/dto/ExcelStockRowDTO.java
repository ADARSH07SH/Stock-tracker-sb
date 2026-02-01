package com.ash.tracker_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExcelStockRowDTO {

    private String stockName;
    private String isin;

    private Integer quantity;
    private Double averageBuyPrice;
}

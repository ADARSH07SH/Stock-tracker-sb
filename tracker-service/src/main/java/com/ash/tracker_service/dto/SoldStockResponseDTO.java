package com.ash.tracker_service.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class SoldStockResponseDTO {

    private String stockName;
    private String isin;

    private Integer quantitySold;
    private Double averageBuyPrice;
    private Double sellPrice;

    private Double investedValue;
    private Double soldValue;
    private Double realisedPL;

    private Instant soldAt;
}

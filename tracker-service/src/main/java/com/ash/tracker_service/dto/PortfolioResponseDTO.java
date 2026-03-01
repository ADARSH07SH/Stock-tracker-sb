package com.ash.tracker_service.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class PortfolioResponseDTO {

    private String userId;
    private String accountId;
    private String accountName;

    private List<StockHoldingResponseDTO> stocks;
    private List<SoldStockResponseDTO> soldStocks;

    private double totalInvestment;
    private double totalCurrentValue;
    private double totalUnrealisedPL;
    private double totalRealisedPL;

    private double stocksValue;
    private double mutualFundsValue;
    private double othersValue;


    private Instant updatedAt;
}

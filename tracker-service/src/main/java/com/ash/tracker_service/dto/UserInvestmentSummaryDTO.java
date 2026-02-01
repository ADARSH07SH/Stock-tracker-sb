package com.ash.tracker_service.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserInvestmentSummaryDTO {

    private String userId;

    private double totalInvestment;
    private double totalCurrentValue;

    private double totalUnrealisedPL;
    private double totalRealisedPL;

    private double stocksValue;
    private double mutualFundsValue;
    private double othersValue;
}

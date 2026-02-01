package com.ash.tracker_service.entity;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockHolding {

    private String stockName;
    private String isin;

    private Integer quantity;
    private Double averageBuyPrice;
    private Double buyValue;

    private Instant lastUpdated;
}

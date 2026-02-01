package com.ash.tracker_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class SoldStockDTO {

    private String userId;
    private String accountId;

    private String stockName;
    private String isin;

    private Integer quantitySold;
    private Double sellPrice;

    private Instant soldAt;
}

package com.ash.tracker_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class PendingSellItemDTO {

    private String stockName;
    private String isin;

    private Integer quantitySold;
    private Double sellPrice;

    private Instant soldAt;
}

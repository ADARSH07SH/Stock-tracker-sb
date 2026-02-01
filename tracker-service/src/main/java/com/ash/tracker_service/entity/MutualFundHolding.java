package com.ash.tracker_service.entity;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MutualFundHolding {

    private String name;
    private String folio;
    private Double units;
    private Double nav;
    private Double investedValue;
    private Double currentValue;
    private Instant lastUpdated;
}

package com.ash.tracker_service.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "sold_stocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoldStock {

    @Id
    private String id;

    private String userId;
    private String accountId;

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

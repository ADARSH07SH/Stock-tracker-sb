package com.ash.tracker_service.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "user_portfolios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPortfolio {

    @Id
    private String id;

    private String userId;
    private String accountId;
    private String accountName;

    private List<StockHolding> stocks;
    private List<MutualFundHolding> mutualFunds;
    private List<OtherAsset> others;

    private double totalInvestment;
    private double totalCurrentValue;

    private Instant updatedAt;
}

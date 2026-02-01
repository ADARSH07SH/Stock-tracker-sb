package com.ash.tracker_service.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "pending_sell_stocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingSellStock {

    @Id
    private String id;

    private String userId;
    private String accountId;

    private String stockName;
    private String isin;

    private Integer existingQuantity;
    private Integer newQuantity;
    private Integer quantityToSell;

    private Instant detectedAt;
}

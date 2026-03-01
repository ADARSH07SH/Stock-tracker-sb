package com.ash.tracker_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PendingSellDTO {

    private String isin;
    private String stockName;

    private Integer existingQuantity;
    private Integer newQuantity;
    private Integer quantityToSell;
}

package com.ash.tracker_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddStockDTO {

        private String userId;
        private String accountId;

        private String stockName;
        private String isin;

        private Integer quantity;
        private Double buyPrice;
}

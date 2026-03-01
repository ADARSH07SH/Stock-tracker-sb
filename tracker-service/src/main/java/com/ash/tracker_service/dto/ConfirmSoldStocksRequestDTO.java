package com.ash.tracker_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ConfirmSoldStocksRequestDTO {
    
    private String userId;
    private String accountId;
    private List<SoldStockConfirmationDTO> soldStocks;
}

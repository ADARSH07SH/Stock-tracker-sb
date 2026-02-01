package com.ash.tracker_service.service;

import com.ash.tracker_service.dto.AddStockDTO;
import com.ash.tracker_service.dto.SoldStockDTO;

public interface PortfolioTransactionService {
    void addStock(AddStockDTO dto);
    void sellStock(SoldStockDTO dto);
}

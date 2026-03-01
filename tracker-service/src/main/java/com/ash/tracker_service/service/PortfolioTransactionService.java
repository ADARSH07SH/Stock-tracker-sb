package com.ash.tracker_service.service;

import com.ash.tracker_service.dto.AddStockDTO;
import com.ash.tracker_service.dto.SoldStockDTO;
import java.util.List;

public interface PortfolioTransactionService {
    void addStock(AddStockDTO dto);
    void sellStock(SoldStockDTO dto);
    List<com.ash.tracker_service.entity.SoldStock> getUserSoldStocks(String userId);
    List<com.ash.tracker_service.entity.SoldStock> getAccountSoldStocks(String userId, String accountId);
}

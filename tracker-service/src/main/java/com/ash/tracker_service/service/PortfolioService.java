package com.ash.tracker_service.service;

import com.ash.tracker_service.dto.ConfirmSoldStocksRequestDTO;
import com.ash.tracker_service.dto.PortfolioResponseDTO;

public interface PortfolioService {
    PortfolioResponseDTO getPortfolio(String userId, String accountId);
    
    java.util.List<PortfolioResponseDTO> getAllPortfolios(String userId);

    void initPortfolio(String userId, String accountId);
    
    void confirmSoldStocks(ConfirmSoldStocksRequestDTO request);
}

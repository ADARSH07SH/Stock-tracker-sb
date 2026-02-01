package com.ash.tracker_service.service;

import com.ash.tracker_service.dto.ExcelStockRowDTO;
import com.ash.tracker_service.dto.ExcelUploadResponseDTO;
import com.ash.tracker_service.dto.SoldStockDetectionDTO;
import com.ash.tracker_service.entity.StockHolding;
import com.ash.tracker_service.entity.UserPortfolio;
import com.ash.tracker_service.repository.UserPortfolioRepository;
import com.ash.tracker_service.util.ExcelParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExcelUploadServiceImpl implements ExcelUploadService {

    private final UserPortfolioRepository userPortfolioRepository;

    @Override
    public Object upload(String userId, String accountId, MultipartFile file, String mode) {

        UserPortfolio portfolio = userPortfolioRepository
                .findByUserIdAndAccountId(userId, accountId)
                .orElseGet(() -> initPortfolio(userId, accountId));

        List<ExcelStockRowDTO> rows = ExcelParser.parse(file);

        // Detect changes between current portfolio and new Excel data
        ExcelUploadResponseDTO response = analyzeAndUpdate(portfolio, rows);

        portfolio.setUpdatedAt(Instant.now());
        recalcInvestment(portfolio);
        userPortfolioRepository.save(portfolio);

        System.out.println("Excel uploaded for account: " + accountId + " with " + rows.size() + " stocks");

        return response;
    }

    private ExcelUploadResponseDTO analyzeAndUpdate(UserPortfolio portfolio, List<ExcelStockRowDTO> newRows) {
        
        // Create maps for easy lookup
        Map<String, StockHolding> currentStocksMap = portfolio.getStocks()
                .stream()
                .collect(Collectors.toMap(StockHolding::getIsin, s -> s));
        
        Map<String, ExcelStockRowDTO> newStocksMap = newRows
                .stream()
                .collect(Collectors.toMap(ExcelStockRowDTO::getIsin, r -> r));
        
        List<StockHolding> updatedHoldings = new ArrayList<>();
        List<SoldStockDetectionDTO> detectedSoldStocks = new ArrayList<>();
        
        int addedCount = 0;
        int updatedCount = 0;
        
        // Process stocks from new Excel
        for (ExcelStockRowDTO newRow : newRows) {
            StockHolding existing = currentStocksMap.get(newRow.getIsin());
            
            if (existing == null) {
                // New stock added
                addedCount++;
            } else {
                // Stock exists - check if quantity or price changed
                if (!existing.getQuantity().equals(newRow.getQuantity()) || 
                    !existing.getAverageBuyPrice().equals(newRow.getAverageBuyPrice())) {
                    updatedCount++;
                }
            }
            
            // Add/update the stock
            updatedHoldings.add(
                StockHolding.builder()
                        .stockName(newRow.getStockName())
                        .isin(newRow.getIsin())
                        .quantity(newRow.getQuantity())
                        .averageBuyPrice(newRow.getAverageBuyPrice())
                        .buyValue(newRow.getAverageBuyPrice() * newRow.getQuantity())
                        .lastUpdated(Instant.now())
                        .build()
            );
        }
        
        // Detect potentially sold stocks (in current portfolio but not in new Excel)
        for (StockHolding current : portfolio.getStocks()) {
            if (!newStocksMap.containsKey(current.getIsin())) {
                detectedSoldStocks.add(
                    SoldStockDetectionDTO.builder()
                            .stockName(current.getStockName())
                            .isin(current.getIsin())
                            .quantity(current.getQuantity())
                            .averageBuyPrice(current.getAverageBuyPrice())
                            .investedValue(current.getBuyValue())
                            .build()
                );
            }
        }
        
        // Update portfolio with new holdings
        portfolio.setStocks(updatedHoldings);
        
        // Build response
        String message;
        if (detectedSoldStocks.isEmpty()) {
            message = "Portfolio updated successfully";
        } else {
            message = "Portfolio updated. " + detectedSoldStocks.size() + " stock(s) appear to be sold. Please confirm.";
        }
        
        return ExcelUploadResponseDTO.builder()
                .status(detectedSoldStocks.isEmpty() ? "SUCCESS" : "NEEDS_CONFIRMATION")
                .totalStocks(newRows.size())
                .addedStocks(addedCount)
                .updatedStocks(updatedCount)
                .potentiallySoldStocks(detectedSoldStocks.size())
                .detectedSoldStocks(detectedSoldStocks)
                .message(message)
                .build();
    }

    private UserPortfolio initPortfolio(String userId, String accountId) {

        UserPortfolio portfolio = UserPortfolio.builder()
                .userId(userId)
                .accountId(accountId)
                .stocks(new ArrayList<>())
                .totalInvestment(0)
                .totalCurrentValue(0)
                .updatedAt(Instant.now())
                .build();

        System.out.println("Initialized portfolio for account: " + accountId);
        return userPortfolioRepository.save(portfolio);
    }

    private void recalcInvestment(UserPortfolio portfolio) {

        portfolio.setTotalInvestment(
                portfolio.getStocks()
                        .stream()
                        .mapToDouble(StockHolding::getBuyValue)
                        .sum()
        );
    }
}

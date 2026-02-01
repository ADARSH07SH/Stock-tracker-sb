package com.ash.tracker_service.service;

import com.ash.tracker_service.dto.*;
import com.ash.tracker_service.entity.*;
import com.ash.tracker_service.repository.SoldStockRepository;
import com.ash.tracker_service.repository.UserPortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

    private final UserPortfolioRepository userPortfolioRepository;
    private final SoldStockRepository soldStockRepository;
    private final MarketPriceService marketPriceService;

    @Override
    public PortfolioResponseDTO getPortfolio(String userId, String accountId) {

        UserPortfolio portfolio = userPortfolioRepository
                .findByUserIdAndAccountId(userId, accountId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found for account: " + accountId));

        List<String> isins = portfolio.getStocks()
                .stream()
                .map(StockHolding::getIsin)
                .collect(Collectors.toList());

        Map<String, Double> prices = marketPriceService.getLatestPrices(isins);

        List<StockHoldingResponseDTO> stockResponses = new ArrayList<>();

        double totalCurrentValue = 0;
        double totalUnrealisedPL = 0;

        for (StockHolding s : portfolio.getStocks()) {
            Double price = prices.get(s.getIsin());
            
            // If price is not available, use average buy price as fallback
            boolean priceUnavailable = false;
            if (price == null) {
                System.out.println("WARNING: No market price available for " + s.getStockName() + " (" + s.getIsin() + "). Using buy price as fallback.");
                price = s.getAverageBuyPrice();
                priceUnavailable = true;
            }
            
            double currentValue = price * s.getQuantity();
            double unrealisedPL = (price - s.getAverageBuyPrice()) * s.getQuantity();
            double returnPercentage = ((price - s.getAverageBuyPrice()) / s.getAverageBuyPrice()) * 100;

            totalCurrentValue += currentValue;
            totalUnrealisedPL += unrealisedPL;

            stockResponses.add(
                    StockHoldingResponseDTO.builder()
                            .stockName(s.getStockName())
                            .isin(s.getIsin())
                            .quantity(s.getQuantity())
                            .averageBuyPrice(s.getAverageBuyPrice())
                            .currentPrice(price)
                            .currentValue(currentValue)
                            .unrealisedPL(unrealisedPL)
                            .returnPercentage(returnPercentage)
                            .priceUnavailable(priceUnavailable)
                            .build()
            );
        }

        // Sort stocks by name
        stockResponses.sort(Comparator.comparing(StockHoldingResponseDTO::getStockName));

        List<SoldStock> soldStocks = soldStockRepository
                .findByUserIdAndAccountId(userId, accountId);

        double totalRealisedPL = soldStocks.stream()
                .mapToDouble(SoldStock::getRealisedPL)
                .sum();

        List<SoldStockResponseDTO> soldResponses = soldStocks.stream()
                .map(s -> SoldStockResponseDTO.builder()
                        .stockName(s.getStockName())
                        .isin(s.getIsin())
                        .quantitySold(s.getQuantitySold())
                        .averageBuyPrice(s.getAverageBuyPrice())
                        .sellPrice(s.getSellPrice())
                        .investedValue(s.getInvestedValue())
                        .soldValue(s.getSoldValue())
                        .realisedPL(s.getRealisedPL())
                        .soldAt(s.getSoldAt())
                        .build())
                .toList();

        return PortfolioResponseDTO.builder()
                .userId(userId)
                .accountId(accountId)
                .accountName(portfolio.getAccountName())
                .stocks(stockResponses)
                .soldStocks(soldResponses)
                .totalInvestment(portfolio.getTotalInvestment())
                .totalCurrentValue(totalCurrentValue)
                .totalUnrealisedPL(totalUnrealisedPL)
                .totalRealisedPL(totalRealisedPL)
                .updatedAt(portfolio.getUpdatedAt())
                .build();
    }

    @Override
    public void initPortfolio(String userId, String accountId) {
        
        // Check if portfolio already exists
        Optional<UserPortfolio> existing = userPortfolioRepository
                .findByUserIdAndAccountId(userId, accountId);
        
        if (existing.isPresent()) {
            System.out.println("WARNING: Portfolio already exists for account: " + accountId);
            return;
        }

        UserPortfolio portfolio = UserPortfolio.builder()
                .userId(userId)
                .accountId(accountId)
                .stocks(new ArrayList<>())
                .totalInvestment(0.0)
                .totalCurrentValue(0.0)
                .updatedAt(java.time.Instant.now())
                .build();

        userPortfolioRepository.save(portfolio);
        System.out.println("Portfolio initialized for account: " + accountId);
    }

    @Override
    public void confirmSoldStocks(ConfirmSoldStocksRequestDTO request) {
        
        List<SoldStock> soldStocks = new ArrayList<>();
        
        for (SoldStockConfirmationDTO dto : request.getSoldStocks()) {
            double soldValue = dto.getSellPrice() * dto.getQuantity();
            double realisedPL = soldValue - dto.getInvestedValue();
            
            SoldStock soldStock = SoldStock.builder()
                    .userId(request.getUserId())
                    .accountId(request.getAccountId())
                    .stockName(dto.getStockName())
                    .isin(dto.getIsin())
                    .quantitySold(dto.getQuantity())
                    .averageBuyPrice(dto.getAverageBuyPrice())
                    .sellPrice(dto.getSellPrice())
                    .investedValue(dto.getInvestedValue())
                    .soldValue(soldValue)
                    .realisedPL(realisedPL)
                    .soldAt(Instant.now())
                    .build();
            
            soldStocks.add(soldStock);
        }
        
        soldStockRepository.saveAll(soldStocks);
        System.out.println("Confirmed " + soldStocks.size() + " sold stocks for account: " + request.getAccountId());
    }
}

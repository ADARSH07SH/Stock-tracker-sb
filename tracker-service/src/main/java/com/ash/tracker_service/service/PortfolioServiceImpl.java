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
    private final MissingIsinService missingIsinService;

    @Override
    public PortfolioResponseDTO getPortfolio(String userId, String accountId) {
        UserPortfolio portfolio = userPortfolioRepository
                .findByUserIdAndAccountId(userId, accountId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found for account: " + accountId));

        return convertToResponseDTO(portfolio);
    }

    @Override
    public List<PortfolioResponseDTO> getAllPortfolios(String userId) {
        List<UserPortfolio> portfolios = userPortfolioRepository.findByUserId(userId)
                .orElse(Collections.emptyList());

        return portfolios.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    private PortfolioResponseDTO convertToResponseDTO(UserPortfolio portfolio) {
        String userId = portfolio.getUserId();
        String accountId = portfolio.getAccountId();

        List<StockHolding> stocks = portfolio.getStocks() != null ? portfolio.getStocks() : Collections.emptyList();

        List<String> isins = stocks.stream()
                .map(StockHolding::getIsin)
                .collect(Collectors.toList());

        Map<String, Double> prices = marketPriceService.getLatestPrices(isins);

        List<StockHoldingResponseDTO> stockResponses = new ArrayList<>();

        double totalCurrentValue = 0;
        double totalUnrealisedPL = 0;

        for (StockHolding s : stocks) {
            Double price = prices.get(s.getIsin());
            
            boolean priceUnavailable = false;
            if (price == null) {
                System.out.println("WARNING: No market price available for " + s.getStockName() + " (" + s.getIsin() + "). Using buy price as fallback.");
                price = s.getAverageBuyPrice();
                priceUnavailable = true;
                missingIsinService.recordMissingIsin(s.getIsin(), s.getStockName());
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

package com.ash.tracker_service.service;

import com.ash.tracker_service.dto.AddStockDTO;
import com.ash.tracker_service.dto.SoldStockDTO;
import com.ash.tracker_service.entity.*;
import com.ash.tracker_service.repository.SoldStockRepository;
import com.ash.tracker_service.repository.UserPortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioTransactionServiceImpl implements PortfolioTransactionService {

    private final UserPortfolioRepository userPortfolioRepository;
    private final SoldStockRepository soldStockRepository;

    @Override
    public void addStock(AddStockDTO dto) {

        UserPortfolio portfolio = userPortfolioRepository
                .findByUserIdAndAccountId(dto.getUserId(), dto.getAccountId())
                .orElseThrow();

        if (portfolio.getStocks() == null) {
            portfolio.setStocks(new ArrayList<>());
        }

        StockHolding holding = portfolio.getStocks()
                .stream()
                .filter(s -> s.getIsin().equals(dto.getIsin()))
                .findFirst()
                .orElse(null);

        if (holding != null) {
            int newQty = holding.getQuantity() + dto.getQuantity();
            double totalValue =
                    (holding.getAverageBuyPrice() * holding.getQuantity())
                            + (dto.getBuyPrice() * dto.getQuantity());

            holding.setQuantity(newQty);
            holding.setAverageBuyPrice(totalValue / newQty);
            holding.setBuyValue(holding.getAverageBuyPrice() * newQty);
            holding.setLastUpdated(Instant.now());
        } else {
            portfolio.getStocks().add(
                    StockHolding.builder()
                            .stockName(dto.getStockName())
                            .isin(dto.getIsin())
                            .quantity(dto.getQuantity())
                            .averageBuyPrice(dto.getBuyPrice())
                            .buyValue(dto.getBuyPrice() * dto.getQuantity())
                            .lastUpdated(Instant.now())
                            .build()
            );
        }

        recalcInvestment(portfolio);
        portfolio.setUpdatedAt(Instant.now());
        userPortfolioRepository.save(portfolio);
    }

    @Override
    public void sellStock(SoldStockDTO dto) {

        UserPortfolio portfolio = userPortfolioRepository
                .findByUserIdAndAccountId(dto.getUserId(), dto.getAccountId())
                .orElseThrow();

        StockHolding holding = portfolio.getStocks()
                .stream()
                .filter(s -> s.getIsin().equals(dto.getIsin()))
                .findFirst()
                .orElseThrow();

        double investedValue = holding.getAverageBuyPrice() * dto.getQuantitySold();
        double soldValue = dto.getSellPrice() * dto.getQuantitySold();

        soldStockRepository.save(
                SoldStock.builder()
                        .userId(dto.getUserId())
                        .accountId(dto.getAccountId())
                        .stockName(dto.getStockName())
                        .isin(dto.getIsin())
                        .quantitySold(dto.getQuantitySold())
                        .averageBuyPrice(holding.getAverageBuyPrice())
                        .sellPrice(dto.getSellPrice())
                        .investedValue(investedValue)
                        .soldValue(soldValue)
                        .realisedPL(soldValue - investedValue)
                        .soldAt(dto.getSoldAt())
                        .build()
        );

        int remaining = holding.getQuantity() - dto.getQuantitySold();

        if (remaining == 0) {
            portfolio.getStocks().remove(holding);
        } else {
            holding.setQuantity(remaining);
            holding.setBuyValue(holding.getAverageBuyPrice() * remaining);
            holding.setLastUpdated(Instant.now());
        }

        recalcInvestment(portfolio);
        portfolio.setUpdatedAt(Instant.now());
        userPortfolioRepository.save(portfolio);
    }

    private void recalcInvestment(UserPortfolio portfolio) {
        portfolio.setTotalInvestment(
                portfolio.getStocks()
                        .stream()
                        .mapToDouble(StockHolding::getBuyValue)
                        .sum()
        );
    }
    
    @Override
    public List<SoldStock> getUserSoldStocks(String userId) {
        return soldStockRepository.findByUserId(userId);
    }
    
    @Override
    public List<SoldStock> getAccountSoldStocks(String userId, String accountId) {
        return soldStockRepository.findByUserIdAndAccountId(userId, accountId);
    }
}

package com.ash.tracker_service.service;

import com.ash.tracker_service.dto.UserInvestmentSummaryDTO;
import com.ash.tracker_service.entity.SoldStock;
import com.ash.tracker_service.entity.StockHolding;
import com.ash.tracker_service.entity.UserPortfolio;
import com.ash.tracker_service.repository.SoldStockRepository;
import com.ash.tracker_service.repository.UserPortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserInvestmentSummaryServiceImpl implements UserInvestmentSummaryService {

    private final UserPortfolioRepository userPortfolioRepository;
    private final SoldStockRepository soldStockRepository;
    private final MarketPriceService marketPriceService;

    @Override
    public UserInvestmentSummaryDTO getSummary(String userId) {

        Optional<List<UserPortfolio>> portfolios =
                userPortfolioRepository.findByUserId(userId);

        double totalInvestment = 0;
        double totalCurrentValue = 0;
        double stocksValue = 0;

        for (UserPortfolio p : portfolios.orElse(null)) {
            totalInvestment += p.getTotalInvestment();

            if (p.getStocks() != null && !p.getStocks().isEmpty()) {
                var prices = marketPriceService.getLatestPrices(
                        p.getStocks().stream().map(StockHolding::getIsin).toList()
                );

                for (StockHolding s : p.getStocks()) {
                    double current = prices.get(s.getIsin()) * s.getQuantity();
                    totalCurrentValue += current;
                    stocksValue += current;
                }
            }
        }

        double realisedPL = soldStockRepository.findByUserId(userId)
                .stream()
                .mapToDouble(SoldStock::getRealisedPL)
                .sum();

        return UserInvestmentSummaryDTO.builder()
                .userId(userId)
                .totalInvestment(totalInvestment)
                .totalCurrentValue(totalCurrentValue)
                .totalUnrealisedPL(totalCurrentValue - totalInvestment)
                .totalRealisedPL(realisedPL)
                .stocksValue(stocksValue)
                .mutualFundsValue(0)
                .othersValue(0)
                .build();
    }
}

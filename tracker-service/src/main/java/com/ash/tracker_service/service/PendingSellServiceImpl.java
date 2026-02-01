package com.ash.tracker_service.service;

import com.ash.tracker_service.dto.PendingSellConfirmationDTO;
import com.ash.tracker_service.dto.PendingSellItemDTO;
import com.ash.tracker_service.entity.SoldStock;
import com.ash.tracker_service.entity.StockHolding;
import com.ash.tracker_service.entity.UserPortfolio;
import com.ash.tracker_service.repository.SoldStockRepository;
import com.ash.tracker_service.repository.UserPortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PendingSellServiceImpl implements PendingSellService {

    private final UserPortfolioRepository userPortfolioRepository;
    private final SoldStockRepository soldStockRepository;

    @Override
    public void confirm(PendingSellConfirmationDTO dto) {

        UserPortfolio portfolio = userPortfolioRepository
                .findByUserIdAndAccountId(dto.getUserId(), dto.getAccountId())
                .orElseThrow();

        for (PendingSellItemDTO item : dto.getSells()) {

            StockHolding holding = portfolio.getStocks()
                    .stream()
                    .filter(s -> s.getIsin().equals(item.getIsin()))
                    .findFirst()
                    .orElseThrow();

            double investedValue = holding.getAverageBuyPrice() * item.getQuantitySold();
            double soldValue = item.getSellPrice() * item.getQuantitySold();

            soldStockRepository.save(
                    SoldStock.builder()
                            .userId(dto.getUserId())
                            .accountId(dto.getAccountId())
                            .stockName(item.getStockName())
                            .isin(item.getIsin())
                            .quantitySold(item.getQuantitySold())
                            .averageBuyPrice(holding.getAverageBuyPrice())
                            .sellPrice(item.getSellPrice())
                            .investedValue(investedValue)
                            .soldValue(soldValue)
                            .realisedPL(soldValue - investedValue)
                            .soldAt(
                                    item.getSoldAt() != null
                                            ? item.getSoldAt()
                                            : Instant.now()
                            )
                            .build()
            );

            int remainingQty = holding.getQuantity() - item.getQuantitySold();

            if (remainingQty <= 0) {
                portfolio.getStocks().remove(holding);
            } else {
                holding.setQuantity(remainingQty);
                holding.setBuyValue(holding.getAverageBuyPrice() * remainingQty);
                holding.setLastUpdated(Instant.now());
            }
        }

        portfolio.setTotalInvestment(
                portfolio.getStocks()
                        .stream()
                        .mapToDouble(StockHolding::getBuyValue)
                        .sum()
        );

        portfolio.setUpdatedAt(Instant.now());
        userPortfolioRepository.save(portfolio);
    }
}

package com.ash.tracker_service.controller;

import com.ash.tracker_service.dto.AddStockDTO;
import com.ash.tracker_service.dto.SoldStockDTO;
import com.ash.tracker_service.service.PortfolioTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portfolio/transaction")
@RequiredArgsConstructor
public class PortfolioTransactionController {

    private final PortfolioTransactionService portfolioTransactionService;

    @PostMapping("/buy")
    public void addStock(@RequestBody AddStockDTO dto) {
        portfolioTransactionService.addStock(dto);
    }

    @PostMapping("/sell")
    public void sellStock(@RequestBody SoldStockDTO dto) {
        portfolioTransactionService.sellStock(dto);
    }
}

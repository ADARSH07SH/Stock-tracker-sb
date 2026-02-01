package com.ash.tracker_service.controller;

import com.ash.tracker_service.dto.ConfirmSoldStocksRequestDTO;
import com.ash.tracker_service.dto.PortfolioResponseDTO;
import com.ash.tracker_service.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @PostMapping("/init")
    public void initPortfolio(
            @RequestParam String userId,
            @RequestParam String accountId
    ) {
        portfolioService.initPortfolio(userId, accountId);
    }

    @GetMapping
    public PortfolioResponseDTO getPortfolio(
            @RequestParam String userId,
            @RequestParam String accountId
    ) {
        return portfolioService.getPortfolio(userId, accountId);
    }

    @PostMapping("/confirm-sold")
    public void confirmSoldStocks(@RequestBody ConfirmSoldStocksRequestDTO request) {
        portfolioService.confirmSoldStocks(request);
    }
}

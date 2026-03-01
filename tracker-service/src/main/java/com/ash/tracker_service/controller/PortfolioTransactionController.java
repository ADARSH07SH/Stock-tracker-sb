package com.ash.tracker_service.controller;

import com.ash.tracker_service.dto.AddStockDTO;
import com.ash.tracker_service.dto.SoldStockDTO;
import com.ash.tracker_service.entity.SoldStock;
import com.ash.tracker_service.service.PortfolioTransactionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio/transaction")
@RequiredArgsConstructor
public class PortfolioTransactionController {

    private final PortfolioTransactionService portfolioTransactionService;

    @PostMapping("/buy")
    public ResponseEntity<Void> addStock(@RequestBody AddStockDTO dto, HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        dto.setUserId(userId);
        portfolioTransactionService.addStock(dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sell")
    public ResponseEntity<Void> sellStock(@RequestBody SoldStockDTO dto, HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        dto.setUserId(userId);
        portfolioTransactionService.sellStock(dto);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/history")
    public ResponseEntity<List<SoldStock>> getUserTransactionHistory(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        List<SoldStock> history = portfolioTransactionService.getUserSoldStocks(userId);
        return ResponseEntity.ok(history);
    }
    
    @GetMapping("/history/account/{accountId}")
    public ResponseEntity<List<SoldStock>> getAccountTransactionHistory(
            @PathVariable String accountId,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        List<SoldStock> history = portfolioTransactionService.getAccountSoldStocks(userId, accountId);
        return ResponseEntity.ok(history);
    }
}

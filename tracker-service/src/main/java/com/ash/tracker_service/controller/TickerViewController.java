package com.ash.tracker_service.controller;

import com.ash.tracker_service.entity.Ticker;
import com.ash.tracker_service.service.TickerViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TickerViewController {

    private final TickerViewService tickerViewService;


    @PostMapping("/api/stock-view/{symbol}")
    public ResponseEntity<Map<String, String>> recordView(@PathVariable String symbol) {
        tickerViewService.incrementViewAsync(symbol);
        return ResponseEntity.accepted().body(Map.of("status", "ok", "symbol", symbol));
    }


    @GetMapping("/api/admin/stock-view/top")
    public ResponseEntity<List<Ticker>> topViewed() {
        return ResponseEntity.ok(tickerViewService.getTopViewed());
    }
}

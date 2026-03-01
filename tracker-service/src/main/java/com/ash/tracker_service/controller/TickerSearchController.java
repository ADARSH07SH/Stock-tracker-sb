package com.ash.tracker_service.controller;

import com.ash.tracker_service.entity.Ticker;
import com.ash.tracker_service.service.TickerSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ticker")
@RequiredArgsConstructor
public class TickerSearchController {

    private final TickerSearchService tickerSearchService;

    @GetMapping("/search/{query}")
    public ResponseEntity<List<Ticker>> search(@PathVariable String query) {
        return ResponseEntity.ok(tickerSearchService.search(query));
    }

    @GetMapping("/symbol/{isin}")
    public ResponseEntity<String> getSymbol(@PathVariable String isin) {
        return ResponseEntity.ok(tickerSearchService.getSymbolByIsin(isin));
    }

}

package com.ash.tracker_service.controller;

import com.ash.tracker_service.entity.StockNewsItem;
import com.ash.tracker_service.service.StockNewsSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class StockNewsController {

    private final StockNewsSyncService stockNewsSyncService;




    @GetMapping("/api/stock-news")
    public ResponseEntity<List<StockNewsItem>> getAll() {
        log.info("Request: GET all cached stock news");
        return ResponseEntity.ok(stockNewsSyncService.getAll());
    }


    @GetMapping("/api/stock-news/{symbol}")
    public ResponseEntity<?> getBySymbol(@PathVariable String symbol) {
        log.info("Request: GET news for symbol [{}]", symbol);
        stockNewsSyncService.refreshIfStaleAsync(symbol);

        StockNewsItem item = stockNewsSyncService.getBySymbol(symbol);
        if (item == null) {
            log.info("No cached news for [{}], returning 'fetching' status", symbol);
            return ResponseEntity.ok(Map.of("status", "fetching", "message", "News fetch in progress, try again shortly"));
        }
        log.info("Returning cached news for [{}] ({} rows)", symbol, item.getRows() != null ? item.getRows().size() : 0);
        return ResponseEntity.ok(item);
    }


    @GetMapping("/api/stock-news/search-name/{name}")
    public ResponseEntity<List<StockNewsItem>> searchByName(@PathVariable String name) {
        return ResponseEntity.ok(stockNewsSyncService.searchByName(name));
    }

    @GetMapping("/api/stock-news/search")
    public ResponseEntity<List<StockNewsItem>> search(@RequestParam String query) {
        return ResponseEntity.ok(stockNewsSyncService.searchNews(query));
    }

    @GetMapping("/api/stock-news/links")
    public ResponseEntity<Map<String, Object>> getStockLinks() {
        return ResponseEntity.ok(stockNewsSyncService.getAvailableStockLinks());
    }

    @GetMapping("/api/stock-news/sheet-news/{name}")
    public ResponseEntity<Map<String, Object>> getRawSheetNews(@PathVariable String name) {
        return ResponseEntity.ok(stockNewsSyncService.getRawSheetNews(name));
    }




    @PostMapping("/api/admin/stock-news/sync")
    public ResponseEntity<Map<String, Object>> triggerSync() {
        int count = stockNewsSyncService.syncAll();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "syncedCount", count,
                "message", "Synced " + count + " stocks from Google Sheets"
        ));
    }
}

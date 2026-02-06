package com.ash.tracker_service.controller;

import com.ash.tracker_service.entity.Watchlist;
import com.ash.tracker_service.entity.WatchlistItem;
import com.ash.tracker_service.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
public class WatchlistController {
    
    private final WatchlistService watchlistService;
    
    @GetMapping
    public ResponseEntity<List<Watchlist>> getUserWatchlists(@RequestParam String userId) {
        return ResponseEntity.ok(watchlistService.getUserWatchlists(userId));
    }
    
    @PostMapping
    public ResponseEntity<Watchlist> createWatchlist(
            @RequestParam String userId,
            @RequestParam String name) {
        return ResponseEntity.ok(watchlistService.createWatchlist(userId, name));
    }
    
    @PutMapping("/{watchlistId}")
    public ResponseEntity<Watchlist> updateWatchlistName(
            @PathVariable String watchlistId,
            @RequestParam String name) {
        return ResponseEntity.ok(watchlistService.updateWatchlistName(watchlistId, name));
    }
    
    @PostMapping("/{watchlistId}/stocks")
    public ResponseEntity<Watchlist> addStock(
            @PathVariable String watchlistId,
            @RequestBody WatchlistItem item) {
        return ResponseEntity.ok(watchlistService.addStockToWatchlist(watchlistId, item));
    }
    
    @DeleteMapping("/{watchlistId}/stocks/{isin}")
    public ResponseEntity<Watchlist> removeStock(
            @PathVariable String watchlistId,
            @PathVariable String isin) {
        return ResponseEntity.ok(watchlistService.removeStockFromWatchlist(watchlistId, isin));
    }
    
    @DeleteMapping("/{watchlistId}")
    public ResponseEntity<Void> deleteWatchlist(@PathVariable String watchlistId) {
        watchlistService.deleteWatchlist(watchlistId);
        return ResponseEntity.ok().build();
    }
}

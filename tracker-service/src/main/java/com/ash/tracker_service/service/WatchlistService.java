package com.ash.tracker_service.service;

import com.ash.tracker_service.entity.Watchlist;
import com.ash.tracker_service.entity.WatchlistItem;
import com.ash.tracker_service.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor

public class WatchlistService {
    
    private final WatchlistRepository watchlistRepository;
    
    public List<Watchlist> getUserWatchlists(String userId) {
        return watchlistRepository.findByUserId(userId);
    }
    
    public Watchlist createWatchlist(String userId, String name) {
        Watchlist watchlist = Watchlist.builder()
                .userId(userId)
                .name(name)
                .stocks(new ArrayList<>())
                .createdAt(Instant.now())
                .build();
        return watchlistRepository.save(watchlist);
    }
    
    public Watchlist updateWatchlistName(String watchlistId, String newName) {
        Watchlist watchlist = watchlistRepository.findById(watchlistId)
                .orElseThrow(() -> new RuntimeException("Watchlist not found"));
        watchlist.setName(newName);
        return watchlistRepository.save(watchlist);
    }
    
    public Watchlist addStockToWatchlist(String watchlistId, WatchlistItem item) {
        Watchlist watchlist = watchlistRepository.findById(watchlistId)
                .orElseThrow(() -> new RuntimeException("Watchlist not found"));
        

        boolean exists = watchlist.getStocks().stream()
                .anyMatch(stock -> java.util.Objects.equals(stock.getIsin(), item.getIsin()));
        
        if (!exists) {
            watchlist.getStocks().add(item);
            return watchlistRepository.save(watchlist);
        }
        
        return watchlist;
    }
    
    public Watchlist removeStockFromWatchlist(String watchlistId, String isin) {
        Watchlist watchlist = watchlistRepository.findById(watchlistId)
                .orElseThrow(() -> new RuntimeException("Watchlist not found"));
        
        watchlist.getStocks().removeIf(stock -> stock.getIsin().equals(isin));
        return watchlistRepository.save(watchlist);
    }
    
    public void deleteWatchlist(String watchlistId) {
        watchlistRepository.deleteById(watchlistId);
    }
}

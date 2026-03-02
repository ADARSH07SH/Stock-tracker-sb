package com.ash.tracker_service.service;

import com.ash.tracker_service.entity.Watchlist;
import com.ash.tracker_service.entity.WatchlistItem;
import com.ash.tracker_service.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WatchlistService {
    
    private final WatchlistRepository watchlistRepository;
    private final YahooMarketService yahooMarketService;
    
    public List<Watchlist> getUserWatchlists(String userId) {
        List<Watchlist> watchlists = watchlistRepository.findByUserId(userId);
        
        for (Watchlist watchlist : watchlists) {
            if (watchlist.getStocks() != null) {
                for (WatchlistItem stock : watchlist.getStocks()) {
                    enrichStockWithMarketData(stock);
                }
            }
        }
        
        return watchlists;
    }
    
    @SuppressWarnings("unchecked")
    private void enrichStockWithMarketData(WatchlistItem stock) {
        try {
            Object response = yahooMarketService.getChartAndQuote(stock.getSymbol(), "1d", "1d");
            
            Map<String, Object> root = (Map<String, Object>) response;
            Map<String, Object> chart = (Map<String, Object>) root.get("chart");
            List<Map<String, Object>> result = (List<Map<String, Object>>) chart.get("result");
            
            if (result != null && !result.isEmpty()) {
                Map<String, Object> data = result.get(0);
                Map<String, Object> meta = (Map<String, Object>) data.get("meta");
                
                if (meta != null) {
                    Object price = meta.get("regularMarketPrice");
                    Object prevClose = meta.get("chartPreviousClose");
                    
                    if (price != null) {
                        double currentPrice = ((Number) price).doubleValue();
                        stock.setLastPrice(currentPrice);
                        
                        if (prevClose != null) {
                            double previousClose = ((Number) prevClose).doubleValue();
                            double change = currentPrice - previousClose;
                            double changePercent = (change / previousClose) * 100;
                            stock.setChangePercent(changePercent);
                        }
                    }
                }
                
                Map<String, Object> indicators = (Map<String, Object>) data.get("indicators");
                if (indicators != null) {
                    List<Map<String, Object>> quote = (List<Map<String, Object>>) indicators.get("quote");
                    if (quote != null && !quote.isEmpty()) {
                        List<Double> closeData = (List<Double>) quote.get(0).get("close");
                        if (closeData != null && !closeData.isEmpty()) {
                            stock.setChart(closeData);
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
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

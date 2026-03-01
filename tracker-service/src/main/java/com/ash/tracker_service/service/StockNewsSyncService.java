package com.ash.tracker_service.service;

import com.ash.tracker_service.entity.StockNewsItem;

import java.util.List;
import java.util.Map;

public interface StockNewsSyncService {

    
    int syncAll();

    List<StockNewsItem> getAll();

    StockNewsItem getBySymbol(String symbol);

    List<StockNewsItem> searchByName(String name);

    List<StockNewsItem> searchNews(String query);

    Map<String, Object> getAvailableStockLinks();

    Map<String, Object> getRawSheetNews(String stockName);

    
    void refreshIfStaleAsync(String symbol);
}


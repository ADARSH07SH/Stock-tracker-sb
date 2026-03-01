package com.ash.tracker_service.repository;

import com.ash.tracker_service.entity.StockNewsItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockNewsRepository extends MongoRepository<StockNewsItem, String> {

    Optional<StockNewsItem> findBySpreadsheetId(String spreadsheetId);

    List<StockNewsItem> findAllByOrderBySyncedAtDesc();

    Optional<StockNewsItem> findBySymbolIgnoreCase(String symbol);

    Optional<StockNewsItem> findByIsinIgnoreCase(String isin);

    List<StockNewsItem> findByStockNameContainingIgnoreCase(String name);
}

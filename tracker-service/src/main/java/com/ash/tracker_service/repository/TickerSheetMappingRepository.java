package com.ash.tracker_service.repository;

import com.ash.tracker_service.entity.TickerSheetMapping;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TickerSheetMappingRepository extends MongoRepository<TickerSheetMapping, String> {

    Optional<TickerSheetMapping> findByIsin(String isin);

    Optional<TickerSheetMapping> findBySymbolIgnoreCase(String symbol);

    Optional<TickerSheetMapping> findBySpreadsheetId(String spreadsheetId);

    List<TickerSheetMapping> findByStatus(String status);

    List<TickerSheetMapping> findAllByOrderByStatusAscSymbolAsc();
}

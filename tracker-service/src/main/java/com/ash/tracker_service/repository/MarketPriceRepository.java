package com.ash.tracker_service.repository;

import com.ash.tracker_service.entity.MarketPrice;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MarketPriceRepository extends MongoRepository<MarketPrice, String> {

    List<MarketPrice> findByIsinIn(List<String> isins);
}

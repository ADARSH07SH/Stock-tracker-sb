package com.ash.tracker_service.repository;

import com.ash.tracker_service.entity.Ticker;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TickerRepository extends MongoRepository<Ticker, String> {

    @Query("{ '$or': [ "
            + "{ 'symbol': { $regex: ?0, $options: 'i' } }, "
            + "{ 'name': { $regex: ?0, $options: 'i' } }, "
            + "{ 'isin': { $regex: ?0, $options: 'i' } } "
            + "] }")
    List<Ticker> searchByQuery(String query);

    Optional<Ticker> findByIsinAndSource(String isin, String source);
}

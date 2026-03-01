package com.ash.tracker_service.repository;

import com.ash.tracker_service.entity.Watchlist;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface WatchlistRepository extends MongoRepository<Watchlist, String> {
    List<Watchlist> findByUserId(String userId);
    Optional<Watchlist> findByUserIdAndName(String userId, String name);
}

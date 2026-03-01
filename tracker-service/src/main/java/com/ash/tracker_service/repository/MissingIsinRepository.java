package com.ash.tracker_service.repository;

import com.ash.tracker_service.entity.MissingIsin;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface MissingIsinRepository extends MongoRepository<MissingIsin, String> {
    Optional<MissingIsin> findByIsin(String isin);
    List<MissingIsin> findByStatus(String status);
}

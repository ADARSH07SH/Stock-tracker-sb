package com.ash.tracker_service.repository;

import com.ash.tracker_service.entity.AppVersion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppVersionRepository extends MongoRepository<AppVersion, String> {
    Optional<AppVersion> findTopByOrderByCreatedAtDesc();
}

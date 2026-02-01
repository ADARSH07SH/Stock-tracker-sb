package com.ash.tracker_service.repository;

import com.ash.tracker_service.entity.TrackerUser;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TrackerUserRepository extends MongoRepository<TrackerUser, String> {
    Optional<TrackerUser> findByUserId(String userId);
    boolean existsByUserId(String userId);
}

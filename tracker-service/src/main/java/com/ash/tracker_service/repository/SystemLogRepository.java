package com.ash.tracker_service.repository;

import com.ash.tracker_service.entity.SystemLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemLogRepository extends MongoRepository<SystemLog, String> {
}

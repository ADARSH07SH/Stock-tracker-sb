package com.ash.tracker_service.repository;

import com.ash.tracker_service.entity.SoldStock;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SoldStockRepository extends MongoRepository<SoldStock,String> {
    List<SoldStock> findByUserIdAndAccountId(String userId, String accountId);
    List<SoldStock> findByUserId(String userId);


}

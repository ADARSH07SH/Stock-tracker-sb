package com.ash.tracker_service.repository;

import com.ash.tracker_service.entity.UserPortfolio;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserPortfolioRepository extends MongoRepository<UserPortfolio, String> {
    Optional<UserPortfolio> findByUserIdAndAccountId(String userId, String accountId);
    Optional <List<UserPortfolio>> findByUserId(String userId);



    boolean existsByUserId(String userId);
}

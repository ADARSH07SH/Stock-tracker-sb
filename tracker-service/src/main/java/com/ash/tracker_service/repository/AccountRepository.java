package com.ash.tracker_service.repository;

import com.ash.tracker_service.entity.Account;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AccountRepository extends MongoRepository<Account, String> {

    List<Account> findByUserId(String userId);
}

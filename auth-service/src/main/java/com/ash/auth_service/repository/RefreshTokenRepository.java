package com.ash.auth_service.repository;

import com.ash.auth_service.entity.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);
}


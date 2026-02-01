package com.ash.auth_service.repository;

import com.ash.auth_service.entity.ForgotPasswordOtp;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ForgotPasswordOtpRepository extends MongoRepository<ForgotPasswordOtp, String> {

    Optional<ForgotPasswordOtp> findFirstByEmailOrderByExpiryTimeDesc(String email);
    Optional<ForgotPasswordOtp> findByEmailAndOtpAndUsedFalse(String email,String otp);

}


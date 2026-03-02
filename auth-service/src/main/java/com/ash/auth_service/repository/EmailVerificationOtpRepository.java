package com.ash.auth_service.repository;

import com.ash.auth_service.entity.EmailVerificationOtp;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface EmailVerificationOtpRepository extends MongoRepository<EmailVerificationOtp, String> {
    Optional<EmailVerificationOtp> findByEmailAndOtp(String email, String otp);
}

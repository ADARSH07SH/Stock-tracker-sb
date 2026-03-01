package com.ash.auth_service.repository;

import com.ash.auth_service.entity.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {
    
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
    
    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);
    
    List<Payment> findByUserId(String userId);
    
    List<Payment> findByStatus(String status);
    
    List<Payment> findByUserIdAndStatus(String userId, String status);
}
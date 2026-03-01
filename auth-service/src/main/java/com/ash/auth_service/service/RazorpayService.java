package com.ash.auth_service.service;

import com.ash.auth_service.entity.Payment;
import com.ash.auth_service.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.util.Base64Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class RazorpayService {

    @Value("${razorpay.key.id:rzp_test_1DP5mmOlF5G5ag}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret:your_razorpay_key_secret_here}")
    private String razorpayKeySecret;

    @Autowired
    private PaymentRepository paymentRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String RAZORPAY_API_URL = "https://api.razorpay.com/v1";

    public Map<String, Object> createOrder(Double amount, String currency, String userId) {
        try {
            String receipt = "receipt_" + System.currentTimeMillis();
            
           
            Map<String, Object> orderRequest = new HashMap<>();
            orderRequest.put("amount", (int)(amount * 100)); 
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", receipt);
            orderRequest.put("payment_capture", 1);


            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String auth = razorpayKeyId + ":" + razorpayKeySecret;
            String encodedAuth = Base64Utils.encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(orderRequest, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                RAZORPAY_API_URL + "/orders", 
                entity, 
                Map.class
            );

            Map<String, Object> orderResponse;
            if (response.getStatusCode() == HttpStatus.OK) {
                orderResponse = response.getBody();
            } else {
             
                orderResponse = createMockOrder(amount, currency, receipt);
            }

           
            Payment payment = new Payment(userId, orderResponse.get("id").toString(), amount, currency, receipt);
            paymentRepository.save(payment);

            return orderResponse;

        } catch (Exception e) {
            
            String receipt = "receipt_" + System.currentTimeMillis();
            Map<String, Object> mockOrder = createMockOrder(amount, currency, receipt);
            
            Payment payment = new Payment(userId, mockOrder.get("id").toString(), amount, currency, receipt);
            paymentRepository.save(payment);
            
            return mockOrder;
        }
    }

    private Map<String, Object> createMockOrder(Double amount, String currency, String receipt) {
        Map<String, Object> mockOrder = new HashMap<>();
        mockOrder.put("id", "order_" + System.currentTimeMillis());
        mockOrder.put("amount", (int)(amount * 100));
        mockOrder.put("currency", currency);
        mockOrder.put("status", "created");
        mockOrder.put("receipt", receipt);
        return mockOrder;
    }

    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            String generatedSignature = calculateHmacSha256(orderId + "|" + paymentId, razorpayKeySecret);
            
            boolean isValid = generatedSignature.equals(signature);
            
            Optional<Payment> paymentOpt = paymentRepository.findByRazorpayOrderId(orderId);
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                payment.setRazorpayPaymentId(paymentId);
                payment.setRazorpaySignature(signature);
                if (isValid) {
                    payment.setStatus("paid");
                } else {
                    payment.setStatus("failed");
                }
                paymentRepository.save(payment);
            } else {
                return false;
            }

            return isValid;
        } catch (Exception e) {
            return false;
        }
    }

    private String calculateHmacSha256(String data, String secret) throws Exception {
        javax.crypto.Mac sha256_HMAC = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec secret_key = new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] bytes = sha256_HMAC.doFinal(data.getBytes());
        StringBuilder buffer = new StringBuilder();
        for (byte b : bytes) {
            buffer.append(String.format("%02x", b));
        }
        return buffer.toString();
    }

    public Map<String, String> getPaymentConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("keyId", razorpayKeyId);
        config.put("currency", "INR");
        config.put("companyName", "Stock Tracker Pro");
        config.put("description", "Support App Development");
        config.put("themeColor", "#3399cc");
        return config;
    }

    public Payment getPaymentByOrderId(String orderId) {
        return paymentRepository.findByRazorpayOrderId(orderId).orElse(null);
    }

    public Payment getPaymentByPaymentId(String paymentId) {
        return paymentRepository.findByRazorpayPaymentId(paymentId).orElse(null);
    }
}
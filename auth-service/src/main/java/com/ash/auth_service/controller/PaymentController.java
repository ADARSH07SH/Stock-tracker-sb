package com.ash.auth_service.controller;

import com.ash.auth_service.dto.ApiResponse;
import com.ash.auth_service.service.RazorpayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired
    private RazorpayService razorpayService;

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<Map<String, String>>> getPaymentConfig() {
        try {
            Map<String, String> config = razorpayService.getPaymentConfig();
            return ResponseEntity.ok(ApiResponse.success(config, "Payment configuration retrieved"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to get payment configuration: " + e.getMessage()));
        }
    }

    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createOrder(@RequestBody Map<String, Object> request) {
        try {
            Double amount = Double.parseDouble(request.get("amount").toString());
            String currency = request.getOrDefault("currency", "INR").toString();
            String userId = request.getOrDefault("userId", "anonymous").toString();
            
            Map<String, Object> order = razorpayService.createOrder(amount, currency, userId);
            
            return ResponseEntity.ok(ApiResponse.success(order, "Order created successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to create order: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-payment")
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyPayment(@RequestBody Map<String, String> request) {
        try {
            String paymentId = request.get("razorpay_payment_id");
            String orderId = request.get("razorpay_order_id");
            String signature = request.get("razorpay_signature");
            
            boolean isValid = razorpayService.verifyPaymentSignature(orderId, paymentId, signature);
            
            Map<String, String> result = new HashMap<>();
            if (isValid) {
                result.put("status", "success");
                result.put("paymentId", paymentId);
                result.put("orderId", orderId);
                result.put("message", "Payment verified successfully");
                return ResponseEntity.ok(ApiResponse.success(result, "Payment verified"));
            } else {
                result.put("status", "failed");
                result.put("message", "Invalid payment signature");
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Payment verification failed"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Payment verification failed: " + e.getMessage()));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object> payload) {
        try {
         
            String event = payload.get("event").toString();
            
            switch (event) {
                case "payment.captured":
                    
                    Map<String, Object> payment = (Map<String, Object>) payload.get("payload");
                   
                    break;
                case "payment.failed":
                    
                    break;
                default:
                   
                    break;
            }
            
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Webhook processing failed");
        }
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Payment service is running", "OK"));
    }
}
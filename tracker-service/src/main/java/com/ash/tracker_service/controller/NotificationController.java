package com.ash.tracker_service.controller;

import com.ash.tracker_service.dto.NotificationBroadcastRequest;
import com.ash.tracker_service.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/register")
    public ResponseEntity<?> registerPushToken(
            HttpServletRequest request,
            @RequestBody Map<String, String> payload) {
        
        String userId = (String) request.getAttribute("userId");
        String pushToken = payload.get("pushToken");
        
        if (pushToken == null || pushToken.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Push token is required"));
        }
        
        notificationService.savePushToken(userId, pushToken);
        return ResponseEntity.ok(Map.of("message", "Push token registered successfully"));
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> broadcastNotification(
            @RequestBody NotificationBroadcastRequest request) {
        
        int sentCount = notificationService.broadcastToAllUsers(
            request.getTitle(),
            request.getBody(),
            request.getData()
        );
        
        return ResponseEntity.ok(Map.of(
            "message", "Notification broadcast initiated",
            "sentTo", sentCount
        ));
    }

    @PostMapping("/send-to-user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> sendToUser(
            @RequestParam String userId,
            @RequestBody NotificationBroadcastRequest request) {
        
        boolean sent = notificationService.sendToUser(
            userId,
            request.getTitle(),
            request.getBody(),
            request.getData()
        );
        
        if (sent) {
            return ResponseEntity.ok(Map.of("message", "Notification sent"));
        } else {
            return ResponseEntity.status(404).body(Map.of("error", "User not found or no push token"));
        }
    }
}

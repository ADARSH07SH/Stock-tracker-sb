package com.ash.tracker_service.service;

import com.ash.tracker_service.entity.TrackerUser;
import com.ash.tracker_service.repository.TrackerUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final TrackerUserRepository trackerUserRepository;
    private final RestTemplate restTemplate;
    
    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    
    @Async
    public void notifyAllUsers(String title, String body, Map<String, Object> data) {
        log.info("Preparing to send notifications to all users: {}", title);
        
        List<TrackerUser> usersWithTokens = trackerUserRepository.findAll().stream()
                .filter(u -> u.getExpoPushToken() != null && !u.getExpoPushToken().isEmpty())
                .collect(Collectors.toList());
                
        if (usersWithTokens.isEmpty()) {
            log.info("No users found with Expo push tokens. Skipping notification.");
            return;
        }
        
        log.info("Found {} users with push tokens", usersWithTokens.size());
        
        List<String> tokens = usersWithTokens.stream()
                .map(TrackerUser::getExpoPushToken)
                .collect(Collectors.toList());
                
        sendExpoNotifications(tokens, title, body, data);
    }
    
    
    private void sendExpoNotifications(List<String> tokens, String title, String body, Map<String, Object> data) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("Accept-Encoding", "gzip, deflate");
            
            List<Map<String, Object>> messages = new ArrayList<>();
            for (String token : tokens) {
                Map<String, Object> message = new HashMap<>();
                message.put("to", token);
                message.put("sound", "default");
                message.put("title", title);
                message.put("body", body);
                
                if (data != null) {
                    message.put("data", data);
                }
                
                messages.add(message);
            }
            
            HttpEntity<List<Map<String, Object>>> request = new HttpEntity<>(messages, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(EXPO_PUSH_URL, request, String.class);
            log.info("Expo notification response: {}", response.getStatusCode());
            
        } catch (Exception e) {
            log.error("Failed to send Expo push notifications: {}", e.getMessage(), e);
        }
    }
}

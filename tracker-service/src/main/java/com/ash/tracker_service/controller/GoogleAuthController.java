package com.ash.tracker_service.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.ash.tracker_service.security.JwtUtil;
import com.ash.tracker_service.entity.User;
import com.ash.tracker_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GoogleAuthController {
    
    private static final String GOOGLE_CLIENT_ID = "103965612723-uafhct6f9v1sqekuc645cnbkjorr28uv.apps.googleusercontent.com";
    
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> request) {
        try {
            String idToken = request.get("idToken");
            
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), 
                    GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(GOOGLE_CLIENT_ID))
                .build();

            GoogleIdToken token = verifier.verify(idToken);
            
            if (token != null) {
                GoogleIdToken.Payload payload = token.getPayload();
                String email = payload.getEmail();
                String name = (String) payload.get("name");
                
                Optional<User> existingUser = userRepository.findByEmail(email);
                User user;
                
                if (existingUser.isPresent()) {
                    user = existingUser.get();
                } else {
                    user = new User();
                    user.setEmail(email);
                    user.setPassword(passwordEncoder.encode(payload.getSubject()));
                    userRepository.save(user);
                }
                
                String jwtToken = jwtUtil.generateToken(
                    Map.of("userId", user.getUserId()),
                    user.getEmail()
                );
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("accessToken", jwtToken);
                response.put("data", Map.of(
                    "userId", user.getUserId(),
                    "email", user.getEmail()
                ));
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid ID token"
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}

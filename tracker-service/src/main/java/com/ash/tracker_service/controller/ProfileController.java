package com.ash.tracker_service.controller;

import com.ash.tracker_service.dto.ProfileResponseDTO;
import com.ash.tracker_service.dto.ProfileUpdateRequestDTO;
import com.ash.tracker_service.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ProfileResponseDTO> getProfile(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        String email = (String) request.getAttribute("email");

        return ResponseEntity.ok(profileService.getProfile(userId, email));

    }

    @PatchMapping
    public ResponseEntity<ProfileResponseDTO> updateProfile(
            HttpServletRequest request,
            @RequestBody ProfileUpdateRequestDTO dto) {

        String userId = (String) request.getAttribute("userId");

        return ResponseEntity.ok(profileService.updateProfile(userId, dto));
    }

    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadProfileImage(
            HttpServletRequest request,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        
        String userId = (String) request.getAttribute("userId");
        
        try {
            String imageUrl = profileService.uploadProfileImage(userId, file);
            return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/sync-google")
    public ResponseEntity<String> syncGoogleProfile(@RequestBody Map<String, String> payload) {
        System.out.println("========================================");
        System.out.println("✓ Received Google profile sync request");
        System.out.println("  Payload: " + payload);
        System.out.println("========================================");
        
        String userId = payload.get("userId");
        String email = payload.get("email");
        String name = payload.get("name");
        String profilePictureUrl = payload.get("profilePictureUrl");

        System.out.println("✓ Extracted data:");
        System.out.println("  User ID: " + userId);
        System.out.println("  Email: " + email);
        System.out.println("  Name: " + name);
        System.out.println("  Profile Picture URL: " + profilePictureUrl);

        try {
            profileService.syncGoogleProfile(userId, email, name, profilePictureUrl);
            System.out.println("✅ Profile sync completed successfully");
            return ResponseEntity.ok("Profile synced successfully");
        } catch (Exception e) {
            System.err.println("✗ Error during profile sync: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to sync profile: " + e.getMessage());
        }
    }



}

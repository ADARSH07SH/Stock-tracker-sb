package com.ash.tracker_service.controller;

import com.ash.tracker_service.dto.ProfileResponseDTO;
import com.ash.tracker_service.dto.ProfileUpdateRequestDTO;
import com.ash.tracker_service.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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



}

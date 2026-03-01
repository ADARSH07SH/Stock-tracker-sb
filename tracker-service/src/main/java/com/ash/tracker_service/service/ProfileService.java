package com.ash.tracker_service.service;

import com.ash.tracker_service.dto.ProfileResponseDTO;
import com.ash.tracker_service.dto.ProfileUpdateRequestDTO;

public interface ProfileService {

    ProfileResponseDTO getProfile(String userID,String email);

    void initProfile(String userID,String email);

    ProfileResponseDTO updateProfile(String userId, ProfileUpdateRequestDTO profileUpdateRequestDTO);

    void syncGoogleProfile(String userId, String email, String name, String profileImageUrl);
    
    String uploadProfileImage(String userId, org.springframework.web.multipart.MultipartFile file) throws java.io.IOException;
}


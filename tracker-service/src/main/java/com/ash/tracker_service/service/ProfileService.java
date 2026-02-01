package com.ash.tracker_service.service;

import com.ash.tracker_service.dto.ProfileResponseDTO;
import com.ash.tracker_service.dto.ProfileUpdateRequestDTO;

public interface ProfileService {

    ProfileResponseDTO getProfile(String userID,String email);

    void initProfile(String userID,String email);

    ProfileResponseDTO updateProfile(String userId, ProfileUpdateRequestDTO profileUpdateRequestDTO);
}

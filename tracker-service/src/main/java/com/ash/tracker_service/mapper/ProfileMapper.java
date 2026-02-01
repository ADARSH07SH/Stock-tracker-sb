package com.ash.tracker_service.mapper;

import com.ash.tracker_service.dto.ProfileResponseDTO;
import com.ash.tracker_service.entity.TrackerUser;

public class ProfileMapper {

    public static ProfileResponseDTO toProfileResponse(TrackerUser user) {
        return ProfileResponseDTO.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .phoneNumber(user.getPhoneNumber())
                .profilePicture(user.getProfilePicture())
                .dateOfBirth(user.getDateOfBirth())
                .bio(user.getBio())
                .panMasked(user.getPanMasked())
                .build();
    }


}

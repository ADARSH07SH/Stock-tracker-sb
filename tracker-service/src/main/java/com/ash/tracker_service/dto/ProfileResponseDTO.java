package com.ash.tracker_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProfileResponseDTO {
    private String userId;
    private String name;
    private String phoneNumber;
    private String profilePicture;
    private LocalDate dateOfBirth;
    private String bio;
    private String panMasked;
}

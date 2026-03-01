package com.ash.tracker_service.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ProfileUpdateRequestDTO {

    private String name;
    private String phoneNumber;
    private String bio;
    private String profilePicture;
    private LocalDate dateOfBirth;
    private String panMasked;
}

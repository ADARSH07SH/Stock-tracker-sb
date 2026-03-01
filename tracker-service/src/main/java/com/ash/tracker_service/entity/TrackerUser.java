package com.ash.tracker_service.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackerUser {

    @Id
    private String id;

    private String userId;

    private String name;
    private String email;
    private String phoneNumber;

    private String profilePicture;
    private LocalDate dateOfBirth;
    private String bio;
    private String panMasked;

    private Instant createdAt;
    private Instant updatedAt;
}

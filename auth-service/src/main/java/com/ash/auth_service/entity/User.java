package com.ash.auth_service.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Set;

@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class User {

    @Id
    private String id;

    private String userId;

    private String email;
    private String phoneNumber;
    private String password;
    private String provider;

    private Set<Role> roles;
    private UserStatus status;

    private Boolean isTwoFactorEnabled;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastLogin;

    public enum Role {
        ROLE_USER, ROLE_ADMIN, ROLE_TRADER, ROLE_NEWS_PROVIDER
    }

    public enum UserStatus {
        ACTIVE, SUSPENDED, DEACTIVATED
    }
}

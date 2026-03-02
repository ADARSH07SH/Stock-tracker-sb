package com.ash.auth_service.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "email_verification_otps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationOtp {
    @Id
    private String id;
    private String email;
    private String otp;
    private Instant expiryTime;
    private boolean used;
}

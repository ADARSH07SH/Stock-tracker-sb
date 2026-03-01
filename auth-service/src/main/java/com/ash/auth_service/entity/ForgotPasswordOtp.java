package com.ash.auth_service.entity;

import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;

import java.time.Instant;

@Document(collection="forgot_password_otp")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ForgotPasswordOtp {

    private String id;

    private String email;

    private String otp;

    private Instant expiryTime;

    private boolean used;
}

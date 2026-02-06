package com.ash.auth_service.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Collections;

@Component
public class GoogleTokenVerifier {

    @Value("${google.oauth.client-id:}")
    private String googleClientId;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    public void init() {
        if (googleClientId == null || googleClientId.isEmpty()) {
            throw new IllegalStateException("Google Client ID is not configured. Please set 'google.oauth.client-id' in application.yaml");
        }

        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    public GoogleIdToken.Payload verify(String idTokenString) {
        try {
            if (idTokenString == null || idTokenString.isEmpty()) {
                throw new RuntimeException("ID token is null or empty");
            }

            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken != null) {
                return idToken.getPayload();
            } else {
                throw new RuntimeException("Invalid ID token - verification failed");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify Google ID token: " + e.getMessage(), e);
        }
    }
}
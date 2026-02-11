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

    @Value("${google.oauth.client-id.web:536290645008-ja4a5b4rfdps78c1pbiudvghtnmamtcu.apps.googleusercontent.com}")
    private String webClientId;

    @Value("${google.oauth.client-id.android:536290645008-kli0hi712vadq6rmssdk2vt0mi4mdlfj.apps.googleusercontent.com}")
    private String androidClientId;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    public void init() {
        verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(androidClientId))
                .build();
    }

    public GoogleTokenPayload verify(String idTokenString) {
        try {
            if (idTokenString == null || idTokenString.isEmpty()) {
                throw new RuntimeException("ID token is null or empty");
            }

            GoogleIdToken idToken = verifier.verify(idTokenString);
            
            if (idToken == null) {
                throw new RuntimeException("Invalid ID token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            String email = payload.getEmail();
            String userId = payload.getSubject();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");
            Boolean emailVerified = payload.getEmailVerified();

            return new GoogleTokenPayload(email, userId, name, pictureUrl, emailVerified);

        } catch (Exception e) {
            throw new RuntimeException("Failed to verify Google ID token: " + e.getMessage(), e);
        }
    }

    public static class GoogleTokenPayload {
        private final String email;
        private final String userId;
        private final String name;
        private final String picture;
        private final Boolean emailVerified;

        public GoogleTokenPayload(String email, String userId, String name, String picture, Boolean emailVerified) {
            this.email = email;
            this.userId = userId;
            this.name = name;
            this.picture = picture;
            this.emailVerified = emailVerified;
        }

        public String getEmail() {
            return email;
        }

        public String getUserId() {
            return userId;
        }

        public String getName() {
            return name;
        }

        public String getPicture() {
            return picture;
        }

        public Boolean getEmailVerified() {
            return emailVerified;
        }
    }
}

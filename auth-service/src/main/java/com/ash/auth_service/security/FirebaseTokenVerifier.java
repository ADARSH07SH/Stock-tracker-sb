package com.ash.auth_service.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class FirebaseTokenVerifier {

    @PostConstruct
    public void init() {
        System.out.println(" Firebase Token Verifier initialized");
    }

    public FirebaseTokenPayload verify(String idTokenString) {
        try {
            if (idTokenString == null || idTokenString.isEmpty()) {
                throw new RuntimeException("ID token is null or empty");
            }

            System.out.println(" Verifying Firebase ID token...");
            System.out.println(" Token length: " + idTokenString.length());

            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idTokenString);

            System.out.println(" Token verified successfully");
            System.out.println(" email: " + decodedToken.getEmail());
            System.out.println(" UID: " + decodedToken.getUid());
            System.out.println(" Name: " + decodedToken.getName());

            return new FirebaseTokenPayload(
                    decodedToken.getEmail(),
                    decodedToken.getUid(),
                    decodedToken.getName(),
                    decodedToken.getPicture()
            );
        } catch (Exception e) {
            System.err.println(" Token verification failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to verify Firebase ID token: " + e.getMessage(), e);
        }
    }

    public static class FirebaseTokenPayload {
        private final String email;
        private final String uid;
        private final String name;
        private final String picture;

        public FirebaseTokenPayload(String email, String uid, String name, String picture) {
            this.email = email;
            this.uid = uid;
            this.name = name;
            this.picture = picture;
        }

        public String getEmail() {
            return email;
        }

        public String getUid() {
            return uid;
        }

        public String getName() {
            return name;
        }

        public String getPicture() {
            return picture;
        }
    }
}

package com.ash.auth_service.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            // Try to load from external path (for production/Docker)
            try {
                FileInputStream serviceAccount = new FileInputStream("/opt/secrets/firebase-service-account.json");
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setProjectId("ardent-pier-485315-b1")
                        .build();

                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options);
                }
                return;
            } catch (Exception e) {
                // Try resources folder for local development
            }

            // Try to load from resources folder (for local development)
            try {
                var serviceAccount = getClass().getClassLoader()
                        .getResourceAsStream("firebase-service-account.json");
                
                if (serviceAccount != null) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .setProjectId("ardent-pier-485315-b1")
                            .build();

                    if (FirebaseApp.getApps().isEmpty()) {
                        FirebaseApp.initializeApp(options);
                    }
                    return;
                }
            } catch (Exception e) {
                // Try fallback
            }

            // Fallback to application default credentials
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .setProjectId("ardent-pier-485315-b1")
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize Firebase Admin SDK", e);
        }
    }
}

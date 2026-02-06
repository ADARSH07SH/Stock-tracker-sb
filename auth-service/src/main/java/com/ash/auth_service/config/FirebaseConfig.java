package com.ash.auth_service.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            System.out.println(" Initializing Firebase Admin SDK...");


            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .setProjectId("ardent-pier-485315-b1")
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println(" Firebase Admin SDK initialized successfully");
                System.out.println(" Project ID: ardent-pier-485315-b1");
            } else {
                System.out.println("ℹ Firebase Admin SDK already initialized");
            }
        } catch (IOException e) {
            System.err.println(" Failed to initialize Firebase Admin SDK");
            System.err.println(" Make sure GOOGLE_APPLICATION_CREDENTIALS is set");
            System.err.println(" Or place service account key in resources folder");
            e.printStackTrace();
        }
    }
}

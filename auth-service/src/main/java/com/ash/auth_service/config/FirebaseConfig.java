package com.ash.auth_service.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = null;
                
                // trying to load form the opt secrext ec2
                try {
                    serviceAccount = new FileInputStream("/opt/secrets/firebase-service-account.json");
                } catch (Exception e) {
                    // If not found, tryong the class path local)
                    try {
                        serviceAccount = new ClassPathResource("firebase-service-account.json").getInputStream();
                        System.out.println(" Loaded Firebase config from classpath");
                    } catch (Exception ex) {
                        throw new RuntimeException(" Firebase service account file not found in /opt/secrets/ or classpath", ex);
                    }
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setProjectId("ardent-pier-485315-b1")
                        .build();

                FirebaseApp.initializeApp(options);
            }
        } catch (Exception e) {
            System.err.println(" Failed to initialize Firebase Admin SDK: " + e.getMessage());
            throw new RuntimeException("Failed to initialize Firebase Admin SDK", e);
        }
    }
}

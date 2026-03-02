package com.ash.tracker_service.controller;

import com.ash.tracker_service.entity.AppVersion;
import com.ash.tracker_service.service.AppVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/version")
@RequiredArgsConstructor
@Slf4j
public class VersionController {

    private final RestTemplate restTemplate;
    private final AppVersionService appVersionService;

    @Value("${app.version:3.0.1}")
    private String currentVersion;

    @Value("${app.github.repo:ADARSH07SH/Stock-tracker-sb}")
    private String githubRepo;

    @GetMapping("/current")
    public ResponseEntity<Map<String, String>> getCurrentVersion() {
        Map<String, String> response = new HashMap<>();
        response.put("version", currentVersion);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkForUpdates() {
        try {
            AppVersion latestVersion = appVersionService.getLatestVersion();
            
            boolean updateAvailable = isNewerVersion(currentVersion, latestVersion.getVersion());
            
            Map<String, Object> response = new HashMap<>();
            response.put("currentVersion", currentVersion);
            response.put("latestVersion", latestVersion.getVersion());
            response.put("updateAvailable", updateAvailable);
            response.put("downloadUrl", latestVersion.getDownloadUrl());
            response.put("releaseNotes", latestVersion.getReleaseNotes());
            response.put("forceUpdate", latestVersion.isForceUpdate());
            response.put("publishedAt", latestVersion.getCreatedAt());

            log.info(" Update check complete. Current: {}, Latest: {}, Update available: {}", 
                currentVersion, latestVersion.getVersion(), updateAvailable);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error(" Failed to check for updates: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                "currentVersion", currentVersion,
                "updateAvailable", false,
                "error", "Failed to check for updates"
            ));
        }
    }

    private boolean isNewerVersion(String current, String latest) {
        try {

            current = current.replaceFirst("^v", "");
            latest = latest.replaceFirst("^v", "");

            String[] currentParts = current.split("\\.");
            String[] latestParts = latest.split("\\.");

            int length = Math.max(currentParts.length, latestParts.length);
            for (int i = 0; i < length; i++) {
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;

                if (latestPart > currentPart) {
                    return true;
                } else if (latestPart < currentPart) {
                    return false;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Error comparing versions: {}", e.getMessage());
            return false;
        }
    }
}

package com.ash.tracker_service.controller;

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

    @Value("${app.version:3.0.1}")
    private String currentVersion;

    @Value("${app.github.repo:adarsh07sh/Stock-Tracker}")
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
            String url = "https://api.github.com/repos/" + githubRepo + "/releases/latest";
            log.info("Checking for updates from: {}", url);

            Map<String, Object> release = restTemplate.getForObject(url, Map.class);
            
            if (release != null) {
                String latestVersion = (String) release.get("tag_name");
                String downloadUrl = null;
                String releaseNotes = (String) release.get("body");
                String publishedAt = (String) release.get("published_at");
                

                if (release.get("assets") instanceof java.util.List) {
                    java.util.List<Map<String, Object>> assets = (java.util.List<Map<String, Object>>) release.get("assets");
                    for (Map<String, Object> asset : assets) {
                        String name = (String) asset.get("name");
                        if (name != null && name.endsWith(".apk")) {
                            downloadUrl = (String) asset.get("browser_download_url");
                            break;
                        }
                    }
                }

                boolean updateAvailable = isNewerVersion(currentVersion, latestVersion);

                Map<String, Object> response = new HashMap<>();
                response.put("currentVersion", currentVersion);
                response.put("latestVersion", latestVersion);
                response.put("updateAvailable", updateAvailable);
                response.put("downloadUrl", downloadUrl);
                response.put("releaseNotes", releaseNotes);
                response.put("publishedAt", publishedAt);

                log.info("✅ Update check complete. Current: {}, Latest: {}, Update available: {}", 
                    currentVersion, latestVersion, updateAvailable);

                return ResponseEntity.ok(response);
            }

            return ResponseEntity.ok(Map.of(
                "currentVersion", currentVersion,
                "updateAvailable", false
            ));

        } catch (Exception e) {
            log.error("❌ Failed to check for updates: {}", e.getMessage());
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

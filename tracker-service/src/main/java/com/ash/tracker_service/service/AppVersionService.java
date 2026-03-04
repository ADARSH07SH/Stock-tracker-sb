package com.ash.tracker_service.service;

import com.ash.tracker_service.entity.AppVersion;
import com.ash.tracker_service.repository.AppVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppVersionService {
    
    private final AppVersionRepository appVersionRepository;
    
    private static final String GITHUB_RELEASE_BASE = "https://github.com/ADARSH07SH/Stock-tracker-sb/releases/download/";
    
    private static String buildDownloadUrl(String version) {
        return GITHUB_RELEASE_BASE + version + "/app-release.apk";
    }
    
    public AppVersion getLatestVersion() {
        return appVersionRepository.findTopByOrderByCreatedAtDesc()
                .orElseGet(() -> {
                    AppVersion defaultVersion = new AppVersion(
                        "3.1.0",
                        buildDownloadUrl("3.1.0"),
                        "Initial release",
                        false
                    );
                    return appVersionRepository.save(defaultVersion);
                });
    }
    
    public AppVersion updateVersion(String version, String downloadUrl, String releaseNotes, boolean forceUpdate) {
        AppVersion appVersion = new AppVersion();
        appVersion.setVersion(version);
        appVersion.setDownloadUrl(downloadUrl != null && !downloadUrl.isEmpty() ? downloadUrl : buildDownloadUrl(version));
        appVersion.setReleaseNotes(releaseNotes);
        appVersion.setForceUpdate(forceUpdate);
        appVersion.setCreatedAt(LocalDateTime.now());
        appVersion.setUpdatedAt(LocalDateTime.now());
        
        log.info("Creating new app version: {} with download URL: {}", version, appVersion.getDownloadUrl());
        return appVersionRepository.save(appVersion);
    }
}

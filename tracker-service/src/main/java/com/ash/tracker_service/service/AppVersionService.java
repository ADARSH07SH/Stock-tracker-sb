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
    
    private static final String DEFAULT_DOWNLOAD_URL = "https://github.com/ADARSH07SH/Stock-tracker-sb/releases/latest";
    
    public AppVersion getLatestVersion() {
        return appVersionRepository.findTopByOrderByCreatedAtDesc()
                .orElseGet(() -> {
                    AppVersion defaultVersion = new AppVersion(
                        "3.0.1",
                        DEFAULT_DOWNLOAD_URL,
                        "Initial release",
                        false
                    );
                    return appVersionRepository.save(defaultVersion);
                });
    }
    
    public AppVersion updateVersion(String version, String downloadUrl, String releaseNotes, boolean forceUpdate) {
        AppVersion appVersion = new AppVersion();
        appVersion.setVersion(version);
        appVersion.setDownloadUrl(downloadUrl != null && !downloadUrl.isEmpty() ? downloadUrl : DEFAULT_DOWNLOAD_URL);
        appVersion.setReleaseNotes(releaseNotes);
        appVersion.setForceUpdate(forceUpdate);
        appVersion.setCreatedAt(LocalDateTime.now());
        appVersion.setUpdatedAt(LocalDateTime.now());
        
        log.info("Creating new app version: {}", version);
        return appVersionRepository.save(appVersion);
    }
}

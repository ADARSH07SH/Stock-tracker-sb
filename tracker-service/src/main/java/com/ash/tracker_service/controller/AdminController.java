package com.ash.tracker_service.controller;

import com.ash.tracker_service.dto.MissingIsinUpdateRequest;
import com.ash.tracker_service.entity.AppVersion;
import com.ash.tracker_service.entity.MissingIsin;
import com.ash.tracker_service.entity.TrackerUser;
import com.ash.tracker_service.repository.TrackerUserRepository;
import com.ash.tracker_service.repository.UserPortfolioRepository;
import com.ash.tracker_service.repository.SoldStockRepository;
import com.ash.tracker_service.repository.AccountRepository;
import com.ash.tracker_service.service.AppVersionService;
import com.ash.tracker_service.service.MissingIsinService;
import com.ash.tracker_service.service.NotificationService;
import com.ash.tracker_service.service.SystemLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final MissingIsinService missingIsinService;
    private final AppVersionService appVersionService;
    private final NotificationService notificationService;
    private final TrackerUserRepository trackerUserRepository;
    private final UserPortfolioRepository userPortfolioRepository;
    private final SoldStockRepository soldStockRepository;
    private final AccountRepository accountRepository;
    private final SystemLogService systemLogService;
    private final com.ash.tracker_service.repository.SystemSettingRepository systemSettingRepository;

    @GetMapping("/system-logs")
    public ResponseEntity<Map<String, Object>> getSystemLogs(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", systemLogService.getRecentLogs(limit)
        ));
    }

    @GetMapping("/missing-isin")
    public ResponseEntity<List<MissingIsin>> getAllMissingIsin(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(missingIsinService.getMissingIsins(status));
    }

    @PutMapping("/missing-isin/{id}")
    public ResponseEntity<MissingIsin> updateIsin(
            @PathVariable String id,
            @RequestBody MissingIsinUpdateRequest request
    ) {
        return ResponseEntity.ok(
                missingIsinService.updateIsin(id, request)
        );
    }

    @PatchMapping("/missing-isin/{id}/resolve")
    public ResponseEntity<?> markResolved(@PathVariable String id) {
        missingIsinService.markResolved(id);
        return ResponseEntity.ok("ISIN marked as resolved");
    }

    @PostMapping("/missing-isin/record")
    public ResponseEntity<?> recordMissingIsin(@RequestBody MissingIsinUpdateRequest request) {
        missingIsinService.recordMissingIsin(request.getIsin(), request.getStockName());
        return ResponseEntity.ok("Missing ISIN recorded");
    }

    @GetMapping("/app-version")
    public ResponseEntity<AppVersion> getAppVersion() {
        return ResponseEntity.ok(appVersionService.getLatestVersion());
    }

    @GetMapping("/version/latest")
    public ResponseEntity<Map<String, Object>> getLatestVersion() {
        AppVersion latest = appVersionService.getLatestVersion();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", latest
        ));
    }

    @PostMapping("/app-version")
    public ResponseEntity<AppVersion> updateAppVersion(@RequestBody Map<String, Object> request) {
        String version = (String) request.get("version");
        String downloadUrl = (String) request.get("downloadUrl");
        String releaseNotes = (String) request.get("releaseNotes");
        boolean forceUpdate = request.get("forceUpdate") != null ? (boolean) request.get("forceUpdate") : false;
        
        AppVersion updated = appVersionService.updateVersion(version, downloadUrl, releaseNotes, forceUpdate);
        

        notificationService.notifyAllUsers(
            "New Update Available \uD83D\uDE80",
            "Version " + version + " is now available. " + releaseNotes,
            Map.of("type", "VERSION_UPDATE", "version", version)
        );
        
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        long totalUsers = trackerUserRepository.count();
        long usersWithTokens = trackerUserRepository.findAll().stream()
                .filter(u -> u.getExpoPushToken() != null && !u.getExpoPushToken().isEmpty())
                .count();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", Map.of(
                "totalUsers", totalUsers,
                "usersWithPushTokens", usersWithTokens
            )
        ));
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        List<TrackerUser> users = trackerUserRepository.findAll();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", users
        ));
    }

    @DeleteMapping("/users/{userId}")
    @Transactional
    public ResponseEntity<?> deleteUser(@PathVariable String userId) {
        if (!trackerUserRepository.existsByUserId(userId)) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "User not found"
            ));
        }

        trackerUserRepository.deleteByUserId(userId);
        userPortfolioRepository.deleteByUserId(userId);
        accountRepository.deleteByUserId(userId);
        soldStockRepository.deleteByUserId(userId);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "User deleted successfully"
        ));
    }

    @GetMapping("/settings")
    public ResponseEntity<?> getSettings() {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", systemSettingRepository.findAll()
        ));
    }

    @PutMapping("/settings/{key}")
    public ResponseEntity<?> updateSetting(@PathVariable String key, @RequestBody Map<String, String> request) {
        String value = request.get("value");
        com.ash.tracker_service.entity.SystemSetting setting = systemSettingRepository.findById(key)
            .orElse(com.ash.tracker_service.entity.SystemSetting.builder().settingKey(key).build());
        
        setting.setSettingValue(value);
        if (request.containsKey("description")) {
            setting.setDescription(request.get("description"));
        }
        
        systemSettingRepository.save(setting);
        return ResponseEntity.ok(Map.of("success", true, "data", setting));
    }

    @PostMapping("/settings/initialize")
    public ResponseEntity<?> initializeSettings() {
        initializeSetting("STOCK_UPDATE_INTERVAL", "30", "Stock price update interval in seconds");
        initializeSetting("MARKET_HOURS_OVERRIDE", "false", "Override market hours to keep updates always on");
        return ResponseEntity.ok(Map.of("success", true, "message", "Settings initialized"));
    }

    private void initializeSetting(String key, String value, String desc) {
        if (!systemSettingRepository.existsById(key)) {
            systemSettingRepository.save(com.ash.tracker_service.entity.SystemSetting.builder()
                .settingKey(key)
                .settingValue(value)
                .description(desc)
                .build());
        }
    }
}


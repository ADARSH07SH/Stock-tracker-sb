package com.ash.tracker_service.scheduler;

import com.ash.tracker_service.service.NotionNewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotionSyncScheduler {

    private final NotionNewsService notionNewsService;

    // Runs every 5 minutes, but only executes between 6:00 PM and 8:00 PM
    @Scheduled(cron = "0 */5 * * * *")
    public void syncNotionNews() {
        LocalTime now = LocalTime.now();

        // Strictly 18:00 to 19:59 (6 PM – 8 PM window)
        boolean isWithinWindow = !now.isBefore(LocalTime.of(18, 0)) && now.isBefore(LocalTime.of(20, 0));

        if (isWithinWindow) {
            log.info("🔄 Auto-syncing Notion news at {}", now);
            try {
                var result = notionNewsService.syncNotionNews();
                log.info("✅ Auto-sync completed: {}", result);
            } catch (Exception e) {
                log.error("❌ Auto-sync failed: {}", e.getMessage(), e);
            }
        } else {
            log.debug("⏸ Outside Notion sync window (6 PM – 8 PM). Current time: {}", now);
        }
    }
}

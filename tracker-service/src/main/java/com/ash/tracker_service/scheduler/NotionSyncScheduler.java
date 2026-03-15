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

    // Runs every 20 minutes, but only executes between 6:00 PM and 1:00 AM (next day)
    @Scheduled(cron = "0 */20 * * * *")
    public void syncNotionNews() {
        LocalTime now = LocalTime.now();

        // Window: 18:00 (6 PM) to 01:00 (1 AM)
        // Matches if time is >= 18:00 OR < 01:00
        boolean isWithinWindow = !now.isBefore(LocalTime.of(18, 0)) || now.isBefore(LocalTime.of(1, 0));

        if (isWithinWindow) {
            log.info("🔄 Auto-syncing Notion news at {}", now);
            try {
                var result = notionNewsService.syncNotionNews();
                log.info("✅ Auto-sync completed: {}", result);
            } catch (Exception e) {
                log.error("❌ Auto-sync failed: {}", e.getMessage(), e);
            }
        } else {
            log.debug("⏸ Outside Notion sync window (6 PM – 1 AM). Current time: {}", now);
        }
    }
}

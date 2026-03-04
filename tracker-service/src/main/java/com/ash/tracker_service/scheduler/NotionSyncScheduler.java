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


    @Scheduled(cron = "0 */30 * * * *")
    public void syncNotionNews() {
        LocalTime now = LocalTime.now();

        boolean isWithinWindow = now.isAfter(LocalTime.of(17, 59)) || now.isBefore(LocalTime.of(1, 1));

        if (isWithinWindow) {
            log.info("🔄 Auto-syncing Notion news at {}", now);
            try {
                var result = notionNewsService.syncNotionNews();
                log.info("✅ Auto-sync completed: {}", result);
            } catch (Exception e) {
                log.error("❌ Auto-sync failed: {}", e.getMessage(), e);
            }
        } else {
            log.debug("⏭️  Skipping auto-sync (outside 5 PM - 12 AM window). Current time: {}", now);
        }
    }
}

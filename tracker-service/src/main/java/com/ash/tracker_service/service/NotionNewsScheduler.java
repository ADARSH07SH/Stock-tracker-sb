package com.ash.tracker_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Disabled: Replaced by NotionSyncScheduler in the scheduler package,
 * which runs every 5 minutes strictly within the 6 PM – 8 PM window.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotionNewsScheduler {

    private final NotionNewsService notionNewsService;

    // @Scheduled disabled - NotionSyncScheduler (scheduler package) handles this correctly
    public void scheduleNotionSync() {
        log.info("Starting scheduled Notion news sync...");
        notionNewsService.syncNotionNews();
        log.info("Notion news sync completed.");
    }
}

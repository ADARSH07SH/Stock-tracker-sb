package com.ash.tracker_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotionNewsScheduler {

    private final NotionNewsService notionNewsService;

    
    
    
    @Scheduled(cron = "0 0 18-23,0-12 * * *")
    public void scheduleNotionSync() {
        log.info("Starting scheduled Notion news sync...");
        notionNewsService.syncNotionNews();
        log.info("Notion news sync completed.");
    }
}

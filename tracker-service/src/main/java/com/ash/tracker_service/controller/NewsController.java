package com.ash.tracker_service.controller;

import com.ash.tracker_service.entity.NewsArticle;
import com.ash.tracker_service.service.NewsService;
import com.ash.tracker_service.service.NotionNewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;
    private final NotionNewsService notionNewsService;


    @GetMapping("/api/news")
    public ResponseEntity<java.util.Map<String, Object>> getPublishedArticles() {
        List<NewsArticle> adminNews = newsService.getAdminNews();
        List<NewsArticle> notionNews = newsService.getNotionDailyNews();
        
        return ResponseEntity.ok(java.util.Map.of(
            "adminNews", adminNews,
            "notionDailyNews", notionNews
        ));
    }


    @PostMapping("/api/news/notion-sync")
    public ResponseEntity<?> triggerNotionSync() {
        return ResponseEntity.ok(notionNewsService.syncNotionNews());
    }
    

    @PostMapping("/api/admin/news/notion-sync")
    public ResponseEntity<?> triggerNotionSyncAdmin() {
        return ResponseEntity.ok(notionNewsService.syncNotionNews());
    }



    @GetMapping("/api/admin/news/notion")
    public ResponseEntity<List<NewsArticle>> getNotionNewsByType(
            @RequestParam(defaultValue = "DAILY") String type) {
        List<NewsArticle> articles;

        if ("ALL".equalsIgnoreCase(type)) {
            articles = newsService.getAllNotionNews();
        } else if ("SHASHANK".equalsIgnoreCase(type)) {
            articles = newsService.getNotionShashankNews();
        } else {
            articles = newsService.getNotionDailyNews();
        }

        return ResponseEntity.ok(articles);
    }

}

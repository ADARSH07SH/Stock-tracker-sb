package com.ash.tracker_service.controller;

import com.ash.tracker_service.entity.NewsArticle;
import com.ash.tracker_service.repository.NewsRepository;
import com.ash.tracker_service.service.CloudinaryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/news")
@Slf4j
public class AdminNewsController {

    private final NewsRepository newsRepository;
    private final CloudinaryService cloudinaryService;
    
    public AdminNewsController(
            NewsRepository newsRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) CloudinaryService cloudinaryService) {
        this.newsRepository = newsRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping
    public ResponseEntity<List<NewsArticle>> getAllAdminNews() {
        List<NewsArticle> adminNews = newsRepository.findBySourceTypeAndPublishedTrueOrderByCreatedAtDesc("ADMIN");
        return ResponseEntity.ok(adminNews);
    }

    @PostMapping
    public ResponseEntity<NewsArticle> createNews(
            HttpServletRequest request,
            @RequestBody Map<String, String> payload) {
        
        String userId = (String) request.getAttribute("userId");
        log.info("Creating admin news by user: {}", userId);
        
        NewsArticle article = NewsArticle.builder()
                .title(payload.get("title"))
                .summary(payload.get("summary"))
                .body(payload.get("body"))
                .imageUrl(payload.get("imageUrl"))
                .category(payload.getOrDefault("category", "TRENDING"))
                .sourceType("ADMIN")
                .published(true)
                .authorId(userId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .publishedAt(Instant.now())
                .build();
        
        newsRepository.save(article);
        log.info("✅ Admin news created: {}", article.getId());
        
        return ResponseEntity.ok(article);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NewsArticle> updateNews(
            @PathVariable String id,
            @RequestBody Map<String, String> payload) {
        
        NewsArticle article = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News article not found"));
        
        if (payload.containsKey("title")) article.setTitle(payload.get("title"));
        if (payload.containsKey("summary")) article.setSummary(payload.get("summary"));
        if (payload.containsKey("body")) article.setBody(payload.get("body"));
        if (payload.containsKey("imageUrl")) article.setImageUrl(payload.get("imageUrl"));
        if (payload.containsKey("category")) article.setCategory(payload.get("category"));
        
        article.setUpdatedAt(Instant.now());
        newsRepository.save(article);
        
        log.info("✅ Admin news updated: {}", id);
        return ResponseEntity.ok(article);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteNews(@PathVariable String id) {
        NewsArticle article = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News article not found"));
        

        if (cloudinaryService != null && article.getImageUrl() != null && !article.getImageUrl().isEmpty()) {
            try {
                cloudinaryService.deleteImage(article.getImageUrl());
                log.info("🗑️  Deleted image from Cloudinary");
            } catch (Exception e) {
                log.warn("⚠️  Failed to delete image: {}", e.getMessage());
            }
        }
        
        newsRepository.deleteById(id);
        log.info("✅ Admin news deleted: {}", id);
        
        return ResponseEntity.ok(Map.of("message", "News article deleted successfully"));
    }

    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (cloudinaryService == null) {
            log.error("❌ CloudinaryService not available");
            return ResponseEntity.status(500).body(Map.of("error", "Image upload service not configured"));
        }
        
        try {
            log.info("📤 Uploading news image...");
            String imageUrl = cloudinaryService.uploadImage(file, "news_images");
            log.info("✅ News image uploaded: {}", imageUrl);
            return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
        } catch (Exception e) {
            log.error("❌ Failed to upload news image: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}

package com.ash.tracker_service.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "news_articles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsArticle {

    @Id
    private String id;

    private String title;
    private String summary;
    private String body;
    private String imageUrl;
    private String category;
    
    private String sourceType; // "ADMIN" or "NOTION_DAILY"

    private boolean published;

    private String authorId;

    private Instant publishedAt;
    private Instant createdAt;
    private Instant updatedAt;
}

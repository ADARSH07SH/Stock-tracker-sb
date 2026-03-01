package com.ash.tracker_service.service;

import com.ash.tracker_service.dto.NewsDto;
import com.ash.tracker_service.entity.NewsArticle;

import java.util.List;

public interface NewsService {

    List<NewsArticle> getAllArticles();

    List<NewsArticle> getPublishedArticles();

    NewsArticle getArticleById(String id);

    NewsArticle createArticle(NewsDto dto, String authorId);

    NewsArticle updateArticle(String id, NewsDto dto);

    void deleteArticle(String id);

    NewsArticle updateImageUrl(String id, String imageUrl);
    
    List<NewsArticle> getAdminNews();
    
    List<NewsArticle> getNotionDailyNews();


    List<NewsArticle> getNotionShashankNews();

    List<NewsArticle> getAllNotionNews();

}

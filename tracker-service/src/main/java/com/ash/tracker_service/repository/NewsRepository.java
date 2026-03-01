package com.ash.tracker_service.repository;

import com.ash.tracker_service.entity.NewsArticle;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsRepository extends MongoRepository<NewsArticle, String> {

    List<NewsArticle> findAllByOrderByCreatedAtDesc();

    List<NewsArticle> findByPublishedTrueOrderByCreatedAtDesc();
    
    long deleteByAuthorId(String authorId);
    
    long deleteBySourceType(String sourceType);
    
    List<NewsArticle> findBySourceTypeAndPublishedTrueOrderByCreatedAtDesc(String sourceType);
}

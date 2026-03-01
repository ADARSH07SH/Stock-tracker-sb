package com.ash.tracker_service.service;

import com.ash.tracker_service.dto.NewsDto;
import com.ash.tracker_service.entity.NewsArticle;
import com.ash.tracker_service.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;

    @Override
    public List<NewsArticle> getAllArticles() {
        return newsRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public List<NewsArticle> getPublishedArticles() {
        return newsRepository.findByPublishedTrueOrderByCreatedAtDesc();
    }

    @Override
    public NewsArticle getArticleById(String id) {
        return newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article not found: " + id));
    }

    @Override
    public NewsArticle createArticle(NewsDto dto, String authorId) {
        Instant now = Instant.now();
        NewsArticle article = NewsArticle.builder()
                .title(dto.getTitle())
                .summary(dto.getSummary())
                .body(dto.getBody())
                .category(dto.getCategory())
                .published(dto.isPublished())
                .authorId(authorId)
                .createdAt(now)
                .updatedAt(now)
                .publishedAt(dto.isPublished() ? now : null)
                .build();
        return newsRepository.save(article);
    }

    @Override
    public NewsArticle updateArticle(String id, NewsDto dto) {
        NewsArticle article = getArticleById(id);
        article.setTitle(dto.getTitle());
        article.setSummary(dto.getSummary());
        article.setBody(dto.getBody());
        article.setCategory(dto.getCategory());

        
        if (dto.isPublished() && !article.isPublished()) {
            article.setPublishedAt(Instant.now());
        }
        article.setPublished(dto.isPublished());
        article.setUpdatedAt(Instant.now());
        return newsRepository.save(article);
    }

    @Override
    public void deleteArticle(String id) {
        if (!newsRepository.existsById(id)) {
            throw new RuntimeException("Article not found: " + id);
        }
        newsRepository.deleteById(id);
    }

    @Override
    public NewsArticle updateImageUrl(String id, String imageUrl) {
        NewsArticle article = getArticleById(id);
        article.setImageUrl(imageUrl);
        article.setUpdatedAt(Instant.now());
        return newsRepository.save(article);
    }
    
    @Override
    public List<NewsArticle> getAdminNews() {
        return newsRepository.findBySourceTypeAndPublishedTrueOrderByCreatedAtDesc("ADMIN");
    }
    
    @Override
    public List<NewsArticle> getNotionDailyNews() {
        return newsRepository.findBySourceTypeAndPublishedTrueOrderByCreatedAtDesc("NOTION_DAILY");
    }


    @Override
    public List<NewsArticle> getNotionShashankNews() {
        return newsRepository.findBySourceTypeAndPublishedTrueOrderByCreatedAtDesc("NOTION_SHASHANK");
    }

    @Override
    public List<NewsArticle> getAllNotionNews() {
        List<NewsArticle> allNotionNews = new java.util.ArrayList<>();
        allNotionNews.addAll(newsRepository.findBySourceTypeAndPublishedTrueOrderByCreatedAtDesc("NOTION_DAILY"));
        allNotionNews.addAll(newsRepository.findBySourceTypeAndPublishedTrueOrderByCreatedAtDesc("NOTION_SHASHANK"));
        // Sort by createdAt descending
        allNotionNews.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return allNotionNews;
    }

}

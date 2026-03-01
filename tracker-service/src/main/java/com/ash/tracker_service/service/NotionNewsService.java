package com.ash.tracker_service.service;

import com.ash.tracker_service.entity.NewsArticle;
import com.ash.tracker_service.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotionNewsService {

    private final RestTemplate restTemplate;
    private final NewsRepository newsRepository;

    @Value("${notion.api-key}")
    private String notionApiKey;

    @Value("${notion.database-id}")
    private String databaseId;

    @Value("${notion.version}")
    private String notionVersion;

    public Map<String, Object> syncNotionNews() {
        if (notionApiKey == null || notionApiKey.isEmpty() || notionApiKey.contains("PASTE_HERE")) {
            log.warn("Notion API Key not configured correctly.");
            return Map.of("status", "error", "message", "Notion API Key not configured");
        }

        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;

        try {
            String url = "https://api.notion.com/v1/databases/" + databaseId + "/query";
            log.info("Connecting to Notion API: {}", url);
            log.info("Using Database ID: {}", databaseId);
            log.info("Using Notion Version: {}", notionVersion);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + notionApiKey);
            headers.set("Notion-Version", notionVersion);
            headers.set("Content-Type", "application/json");

            
            
            String body = "{\"sorts\": [{\"timestamp\": \"created_time\", \"direction\": \"descending\"}], \"page_size\": 10}";
            log.info("Request Body: {}", body);
            
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            log.info("Notion API Response Status: {}", response.getStatusCode());
            if (response.getBody() != null) {
                Map<String, Object> bodyMap = response.getBody();
                List<Map<String, Object>> results = (List<Map<String, Object>>) bodyMap.get("results");
                
                if (results != null && !results.isEmpty()) {
                    log.info("Found {} results from Notion", results.size());
                    

                    boolean foundNormalArticle = false;
                    for (Map<String, Object> page : results) {
                        try {
                            boolean processed = processNotionPage(page);
                            if (processed) {
                                successCount++;
                                foundNormalArticle = true;
                                log.info("✅ Found and saved latest Normal article, stopping sync");
                                break;
                            } else {
                                skipCount++;
                            }
                        } catch (Exception e) {
                            log.error("Failed to process page: {}", e.getMessage());
                            failCount++;
                        }
                    }
                    
                    if (!foundNormalArticle) {
                        log.warn("⚠️  No Normal type articles found in the latest {} pages", results.size());
                    }
                } else {
                    log.warn("No results found in Notion database query. Response: {}", bodyMap);
                    return Map.of("status", "warning", "message", "Database query returned 0 results");
                }
            } else {
                log.error("Notion API returned empty body.");
                return Map.of("status", "error", "message", "Empty response body from Notion");
            }

            return Map.of(
                "status", "success",
                "synced", successCount,
                "failed", failCount,
                "skipped", skipCount
            );
        } catch (Exception e) {
            log.error("Error syncing news from Notion: {}", e.getMessage(), e);
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    private boolean processNotionPage(Map<String, Object> page) {
        try {
            String notionId = (String) page.get("id");
            String createdTimeStr = (String) page.get("created_time");
            Map<String, Object> properties = (Map<String, Object>) page.get("properties");
            log.info("Processing Notion page ID: {}. Created at: {}. Properties found: {}", notionId, createdTimeStr, properties.keySet());

            
            String title = "Untitled";
            try {
                Map<String, Object> titleProp = (Map<String, Object>) properties.get("Title");
                if (titleProp == null) {
                    log.debug("'Title' property not found, trying 'Name'");
                    titleProp = (Map<String, Object>) properties.get("Name");
                }
                if (titleProp != null) {
                    List<Map<String, Object>> titleList = (List<Map<String, Object>>) titleProp.get("title");
                    if (titleList != null && !titleList.isEmpty()) {
                        title = (String) titleList.get(0).get("plain_text");
                    }
                } else {
                    log.warn("⚠️ No Title or Name property found in Notion page. Available properties: {}", properties.keySet());
                }
            } catch (Exception e) {
                log.warn("Error extracting Title: {}", e.getMessage());
            }
            log.info("✅ Extracted Title: '{}'", title);

            
            Instant articleDate = Instant.now();
            if (createdTimeStr != null) {
                articleDate = Instant.parse(createdTimeStr);
            }

            try {
                Map<String, Object> dateProp = (Map<String, Object>) properties.get("Date");
                if (dateProp != null) {
                    Map<String, Object> dateVal = (Map<String, Object>) dateProp.get("date");
                    if (dateVal != null && dateVal.get("start") != null) {
                        String dateStr = (String) dateVal.get("start");
                        log.info("Extracted Date string: {}", dateStr);
                        
                        articleDate = LocalDate.parse(dateStr).atStartOfDay(ZoneId.systemDefault()).toInstant();
                    }
                } else {
                    log.debug("'Date' property not found in page properties, using created_time.");
                }
            } catch (Exception e) {
                log.warn("Error extracting Date property: {}", e.getMessage());
            }

            
            String summary = "";
            try {
                Map<String, Object> summaryProp = (Map<String, Object>) properties.get("Summary");
                if (summaryProp == null) {
                    log.debug("'Summary' not found, trying 'Text'");
                    summaryProp = (Map<String, Object>) properties.get("Text");
                }
                if (summaryProp == null) {
                    log.debug("'Text' not found, trying 'Description'");
                    summaryProp = (Map<String, Object>) properties.get("Description");
                }
                if (summaryProp == null) {
                    log.debug("'Description' not found, trying 'Content'");
                    summaryProp = (Map<String, Object>) properties.get("Content");
                }
                if (summaryProp != null) {
                    List<Map<String, Object>> richText = (List<Map<String, Object>>) summaryProp.get("rich_text");
                    if (richText != null && !richText.isEmpty()) {
                        summary = (String) richText.get(0).get("plain_text");
                    }
                } else {
                    log.warn("⚠️ No Summary/Text/Description/Content property found. Available properties: {}", properties.keySet());
                }
            } catch (Exception e) {
                log.warn("Error extracting Summary: {}", e.getMessage());
            }
            log.info("✅ Extracted Summary: '{}' (length: {})", 
                summary.length() > 50 ? summary.substring(0, 50) + "..." : summary, 
                summary.length());


            String body = summary;
            if (summary == null || summary.isEmpty()) {
                log.info("📄 Summary is empty, fetching page content blocks...");
                try {
                    body = fetchPageContent(notionId);
                    if (body != null && !body.isEmpty()) {
                        log.info("✅ Fetched page content: '{}' (length: {})", 
                            body.length() > 50 ? body.substring(0, 50) + "..." : body, 
                            body.length());

                        if (summary.isEmpty() && body.length() > 0) {
                            summary = body.length() > 200 ? body.substring(0, 200) + "..." : body;
                        }
                    } else {
                        log.warn("⚠️ No content found in page blocks");
                    }
                } catch (Exception e) {
                    log.error("❌ Failed to fetch page content: {}", e.getMessage());
                }
            } else {
                body = summary;
            }

            
            String source = "NOTION";
            try {
                Map<String, Object> sourceProp = (Map<String, Object>) properties.get("Source");
                if (sourceProp != null) {
                    Map<String, Object> select = (Map<String, Object>) sourceProp.get("select");
                    if (select != null) {
                        source = (String) select.get("name");
                    } else {
                        List<Map<String, Object>> richText = (List<Map<String, Object>>) sourceProp.get("rich_text");
                        if (richText != null && !richText.isEmpty()) {
                            source = (String) richText.get(0).get("plain_text");
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Error extracting Source: {}", e.getMessage());
            }
            log.info("Extracted Source: {}", source);


            String newsType = "Daily-News";
            try {
                Map<String, Object> typeProp = (Map<String, Object>) properties.get("Type");
                if (typeProp != null) {

                    Map<String, Object> select = (Map<String, Object>) typeProp.get("select");
                    if (select != null) {
                        newsType = (String) select.get("name");
                    } else {

                        List<Map<String, Object>> richText = (List<Map<String, Object>>) typeProp.get("rich_text");
                        if (richText != null && !richText.isEmpty()) {
                            newsType = (String) richText.get(0).get("plain_text");
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Error extracting Type: {}", e.getMessage());
            }
            log.info("✅ Extracted Type: '{}'", newsType);


            String sourceType = "NOTION_DAILY";
            if (newsType != null && newsType.equalsIgnoreCase("Shashank")) {
                sourceType = "NOTION_SHASHANK";
                log.info("✅ Processing Shashank type news: '{}'", title);
            } else {
                log.info("✅ Processing Daily-News type: '{}'", title);
            }


            try {
                long deletedCount = newsRepository.deleteBySourceType(sourceType);
                if (deletedCount > 0) {
                    log.info("🗑️  Deleted {} old {} articles", deletedCount, sourceType);
                }
            } catch (Exception e) {
                log.warn("Error deleting old articles: {}", e.getMessage());
            }


            NewsArticle article = NewsArticle.builder()
                    .title(title)
                    .body(body)
                    .sourceType(sourceType)
                    .category("TRENDING")
                    .published(true)
                    .publishedAt(Instant.now())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            newsRepository.save(article);
            log.info("✅ Saved {} article: {}", sourceType, title);

            return true;
        } catch (Exception e) {
            log.error("❌ Error processing Notion page: {}", e.getMessage(), e);
            return false;
        }
    }

    private String fetchPageContent(String pageId) {
        try {
            String url = "https://api.notion.com/v1/blocks/" + pageId + "/children";
            log.info("📄 Fetching page content from: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + notionApiKey);
            headers.set("Notion-Version", notionVersion);
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null) {
                Map<String, Object> bodyMap = response.getBody();
                List<Map<String, Object>> blocks = (List<Map<String, Object>>) bodyMap.get("results");
                
                if (blocks != null && !blocks.isEmpty()) {
                    log.info("📄 Found {} blocks in page", blocks.size());
                    StringBuilder content = new StringBuilder();
                    
                    for (Map<String, Object> block : blocks) {
                        String blockType = (String) block.get("type");
                        log.debug("Processing block type: {}", blockType);
                        

                        if (blockType != null) {
                            Map<String, Object> blockData = (Map<String, Object>) block.get(blockType);
                            if (blockData != null) {
                                List<Map<String, Object>> richText = (List<Map<String, Object>>) blockData.get("rich_text");
                                if (richText != null && !richText.isEmpty()) {
                                    for (Map<String, Object> text : richText) {
                                        String plainText = (String) text.get("plain_text");
                                        if (plainText != null && !plainText.isEmpty()) {
                                            content.append(plainText).append("\n");
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    String result = content.toString().trim();
                    log.info("✅ Extracted {} characters of content", result.length());
                    return result;
                } else {
                    log.warn("⚠️ No blocks found in page");
                }
            }
        } catch (Exception e) {
            log.error("❌ Error fetching page content: {}", e.getMessage(), e);
        }
        return "";
    }
}

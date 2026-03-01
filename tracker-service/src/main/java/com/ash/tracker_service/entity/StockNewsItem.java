package com.ash.tracker_service.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document(collection = "stock_news_cache")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockNewsItem {

    @Id
    private String id;


    private String sheetName;


    private String stockName;


    private String symbol;


    private String isin;


    private String spreadsheetId;


    private String gid;


    private List<Map<String, String>> rows;


    private Instant syncedAt;
}

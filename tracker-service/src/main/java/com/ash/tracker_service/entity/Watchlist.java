package com.ash.tracker_service.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document("watchlists")
public class Watchlist {
    @Id
    private String id;
    private String userId;
    private String name;
    private List<WatchlistItem> stocks;
    private Instant createdAt;
}

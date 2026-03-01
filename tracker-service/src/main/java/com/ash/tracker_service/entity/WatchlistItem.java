package com.ash.tracker_service.entity;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchlistItem {
    private String isin;
    private String symbol;

    
    @JsonAlias({"name", "stockName"})
    @JsonProperty("stockName")
    private String stockName;
}

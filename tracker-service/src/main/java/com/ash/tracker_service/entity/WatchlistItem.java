package com.ash.tracker_service.entity;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

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
    
    private Double lastPrice;
    private Double changePercent;
    private List<Double> chart;
}

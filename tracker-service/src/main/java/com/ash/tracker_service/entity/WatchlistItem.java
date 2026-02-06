package com.ash.tracker_service.entity;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchlistItem {
    private String isin;
    private String symbol;
    private String name;
}

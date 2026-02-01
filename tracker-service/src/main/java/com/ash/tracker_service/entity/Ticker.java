package com.ash.tracker_service.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "tickers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticker {

    @Id
    private String id;

    private String source;
    private String symbol;
    private String name;
    private String isin;
}

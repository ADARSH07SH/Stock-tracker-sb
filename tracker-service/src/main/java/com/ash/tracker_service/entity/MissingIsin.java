package com.ash.tracker_service.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document("missing_isins")
public class MissingIsin {
    @Id
    private String id;
    private String isin;
    private String stockName;
    private String symbol;
    private Instant firstSeenAt;
    private Instant lastSeenAt;
    private Integer occurrenceCount;
    private String status;
}

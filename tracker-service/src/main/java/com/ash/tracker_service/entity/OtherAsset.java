package com.ash.tracker_service.entity;

import lombok.*;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtherAsset {

    private String type;
    private String name;
    private Map<String, Object> details;
    private Double quantity;
    private Double price;
    private Instant lastUpdated;
}

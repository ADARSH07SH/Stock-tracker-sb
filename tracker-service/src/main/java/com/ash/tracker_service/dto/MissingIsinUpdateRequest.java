package com.ash.tracker_service.dto;

import lombok.Data;

@Data
public class MissingIsinUpdateRequest {
    private String isin;
    private String stockName;
    private String symbol;
}

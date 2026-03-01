package com.ash.tracker_service.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "ticker_sheet_mappings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TickerSheetMapping {

    @Id
    private String id;


    private String symbol;
    private String isin;
    private String stockName;


    private String sheetName;


    private String spreadsheetId;


    private String gid;


    private String status;


    private List<SheetCandidate> candidates;


    private List<SheetCandidate> selectedMappings;

    private Instant mappedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SheetCandidate {
        private String sheetName;
        private String spreadsheetId;
        private String gid;
        private int score; 
    }
}

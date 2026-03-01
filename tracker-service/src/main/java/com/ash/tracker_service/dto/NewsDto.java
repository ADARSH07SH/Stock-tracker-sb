package com.ash.tracker_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsDto {
    private String title;
    private String summary;
    private String body;
    private String category;
    private boolean published;
}

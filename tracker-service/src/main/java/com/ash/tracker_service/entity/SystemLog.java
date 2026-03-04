package com.ash.tracker_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "system_logs")
public class SystemLog {
    @Id
    private String id;
    private String eventType;
    private String message;
    private String details;
    private Instant createdAt;
}

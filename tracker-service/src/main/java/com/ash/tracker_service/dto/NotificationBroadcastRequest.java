package com.ash.tracker_service.dto;

import lombok.Data;
import java.util.Map;

@Data
public class NotificationBroadcastRequest {
    private String title;
    private String body;
    private Map<String, Object> data;
}

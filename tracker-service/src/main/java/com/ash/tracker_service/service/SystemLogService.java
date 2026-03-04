package com.ash.tracker_service.service;

import com.ash.tracker_service.entity.SystemLog;
import com.ash.tracker_service.repository.SystemLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemLogService {
    private final SystemLogRepository systemLogRepository;

    public void logSystemEvent(String eventType, String message, String details) {
        try {
            SystemLog logEntry = SystemLog.builder()
                    .eventType(eventType)
                    .message(message)
                    .details(details)
                    .createdAt(Instant.now())
                    .build();
            systemLogRepository.save(logEntry);
            log.info("Saved SystemLog: [{}] {}", eventType, message);
        } catch (Exception e) {
            log.error("Failed to save system log: {}", e.getMessage());
        }
    }

    public List<SystemLog> getRecentLogs(int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SystemLog> page = systemLogRepository.findAll(pageRequest);
        return page.getContent();
    }
}

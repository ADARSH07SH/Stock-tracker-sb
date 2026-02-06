package com.ash.tracker_service.service;

import com.ash.tracker_service.entity.MissingIsin;
import com.ash.tracker_service.repository.MissingIsinRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class MissingIsinService {
    
    private final MissingIsinRepository missingIsinRepository;
    
    public synchronized void recordMissingIsin(String isin, String stockName) {
        try {
            var existing = missingIsinRepository.findByIsin(isin);
            
            if (existing.isPresent()) {
                MissingIsin record = existing.get();
                record.setLastSeenAt(Instant.now());
                record.setOccurrenceCount(record.getOccurrenceCount() + 1);
                missingIsinRepository.save(record);
            } else {
                MissingIsin newRecord = MissingIsin.builder()
                        .isin(isin)
                        .stockName(stockName)
                        .symbol("")
                        .firstSeenAt(Instant.now())
                        .lastSeenAt(Instant.now())
                        .occurrenceCount(1)
                        .status("PENDING")
                        .build();
                missingIsinRepository.save(newRecord);
                log.warn("New missing ISIN recorded: {} - {}", isin, stockName);
            }
        } catch (Exception e) {
            log.error("Error recording missing ISIN: {}", e.getMessage());
        }
    }
}

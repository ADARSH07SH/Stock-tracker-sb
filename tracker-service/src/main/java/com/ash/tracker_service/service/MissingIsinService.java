package com.ash.tracker_service.service;

import com.ash.tracker_service.dto.MissingIsinUpdateRequest;
import com.ash.tracker_service.entity.MissingIsin;
import com.ash.tracker_service.repository.MissingIsinRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

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

    public List<MissingIsin> getMissingIsins(String status) {
        if (status == null) {
            return missingIsinRepository.findAll();
        }
        return missingIsinRepository.findByStatus(status);
    }

    public MissingIsin updateIsin(String id, MissingIsinUpdateRequest request) {
        MissingIsin record = missingIsinRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ISIN record not found"));

        if (request.getIsin() != null) {
            record.setIsin(request.getIsin());
        }
        if (request.getStockName() != null) {
            record.setStockName(request.getStockName());
        }
        if (request.getSymbol() != null) {
            record.setSymbol(request.getSymbol());
        }

        record.setStatus("RESOLVED");
        record.setLastSeenAt(Instant.now());

        return missingIsinRepository.save(record);
    }


    public void markResolved(String id) {
        MissingIsin record = missingIsinRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ISIN record not found"));

        record.setStatus("RESOLVED");
        record.setLastSeenAt(Instant.now());

        missingIsinRepository.save(record);
    }


}

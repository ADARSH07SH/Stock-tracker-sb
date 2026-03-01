package com.ash.tracker_service.controller;

import com.ash.tracker_service.entity.MissingIsin;
import com.ash.tracker_service.entity.Ticker;
import com.ash.tracker_service.repository.MissingIsinRepository;
import com.ash.tracker_service.repository.TickerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/missing-isins")
@RequiredArgsConstructor
public class MissingIsinController {

    private final MissingIsinRepository missingIsinRepository;
    private final TickerRepository tickerRepository;

    @GetMapping
    public ResponseEntity<List<MissingIsin>> getAllMissingIsins() {
        return ResponseEntity.ok(missingIsinRepository.findAll());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<MissingIsin> updateSymbol(
            @PathVariable String id,
            @RequestParam String symbol) {

        MissingIsin record = missingIsinRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Missing ISIN not found"));

        record.setSymbol(symbol);
        record.setStatus("RESOLVED");
        MissingIsin saved = missingIsinRepository.save(record);

        
        boolean alreadyExists = tickerRepository
                .findByIsinAndSource(record.getIsin(), "RESOLVED")
                .isPresent();

        if (!alreadyExists) {
            Ticker ticker = Ticker.builder()
                    .isin(record.getIsin())
                    .symbol(symbol)
                    .name(record.getStockName())
                    .source("RESOLVED")   
                    .build();
            tickerRepository.save(ticker);
            log.info("Added ticker for resolved ISIN: {}  {} ({})",
                    record.getIsin(), symbol, record.getStockName());
        }

        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMissingIsin(@PathVariable String id) {
        missingIsinRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}


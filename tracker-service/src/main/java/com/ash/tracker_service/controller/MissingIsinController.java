package com.ash.tracker_service.controller;

import com.ash.tracker_service.entity.MissingIsin;
import com.ash.tracker_service.repository.MissingIsinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/missing-isins")
@RequiredArgsConstructor
public class MissingIsinController {
    
    private final MissingIsinRepository missingIsinRepository;
    
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
        
        return ResponseEntity.ok(missingIsinRepository.save(record));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMissingIsin(@PathVariable String id) {
        missingIsinRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}

package com.ash.tracker_service.controller;

import com.ash.tracker_service.dto.MissingIsinUpdateRequest;
import com.ash.tracker_service.entity.MissingIsin;
import com.ash.tracker_service.service.MissingIsinService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final MissingIsinService missingIsinService;

    @GetMapping("/missing-isin")
    public ResponseEntity<List<MissingIsin>> getAllMissingIsin(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(missingIsinService.getMissingIsins(status));
    }

    @PutMapping("/missing-isin/{id}")
    public ResponseEntity<MissingIsin> updateIsin(
            @PathVariable String id,
            @RequestBody MissingIsinUpdateRequest request
    ) {
        return ResponseEntity.ok(
                missingIsinService.updateIsin(id, request)
        );
    }

    @PatchMapping("/missing-isin/{id}/resolve")
    public ResponseEntity<?> markResolved(@PathVariable String id) {
        missingIsinService.markResolved(id);
        return ResponseEntity.ok("ISIN marked as resolved");
    }

    @PostMapping("/missing-isin/record")
    public ResponseEntity<?> recordMissingIsin(@RequestBody MissingIsinUpdateRequest request) {
        missingIsinService.recordMissingIsin(request.getIsin(), request.getStockName());
        return ResponseEntity.ok("Missing ISIN recorded");
    }
}

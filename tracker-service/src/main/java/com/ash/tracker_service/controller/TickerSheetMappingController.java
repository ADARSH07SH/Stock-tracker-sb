package com.ash.tracker_service.controller;

import com.ash.tracker_service.entity.TickerSheetMapping;
import com.ash.tracker_service.service.TickerSheetMappingService;
import com.ash.tracker_service.service.TickerSheetMappingService.JobResult;
import com.ash.tracker_service.service.TickerSheetMappingService.JobStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ticker-sheet-mapping")
@RequiredArgsConstructor
public class TickerSheetMappingController {

    private final TickerSheetMappingService service;


    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll() {
        return ResponseEntity.ok(service.getAllEnriched());
    }


    @GetMapping(params = "status")
    public ResponseEntity<List<TickerSheetMapping>> getByStatus(@RequestParam String status) {
        return ResponseEntity.ok(service.getByStatus(status));
    }


    @GetMapping("/unmapped")
    public ResponseEntity<List<Map<String, Object>>> getUnmapped() {
        return ResponseEntity.ok(service.getUnmappedSortedByViewCount());
    }


    @GetMapping("/stock-links")
    public ResponseEntity<List<Map<String, String>>> getStockLinks() {
        return ResponseEntity.ok(service.getAvailableSheets());
    }


    @PostMapping("/auto-map")
    public ResponseEntity<Map<String, String>> autoMap() {
        JobResult job = service.getJobStatus();
        if (job.status == JobStatus.RUNNING) {
            return ResponseEntity.accepted()
                    .body(Map.of("status", "RUNNING", "message", "Auto-map already in progress"));
        }
        service.autoMapAsync();
        return ResponseEntity.accepted()
                .body(Map.of("status", "STARTED", "message", "Auto-map started. Poll /job-status for progress."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh() {
        JobResult job = service.getJobStatus();
        if (job.status == JobStatus.RUNNING) {
            return ResponseEntity.accepted()
                    .body(Map.of("status", "RUNNING", "message", "A job is already running"));
        }
        service.refreshAsync();
        return ResponseEntity.accepted()
                .body(Map.of("status", "STARTED", "message", "Refresh started  only unmapped tickers will be processed."));
    }

    @DeleteMapping("/all")
    public ResponseEntity<Map<String, Object>> deleteAll() {
        long deleted = service.deleteAll();
        return ResponseEntity.ok(Map.of("deleted", deleted, "message", "All mappings cleared"));
    }

    @PostMapping("/restart")
    public ResponseEntity<Map<String, String>> restart() {
        JobResult job = service.getJobStatus();
        if (job.status == JobStatus.RUNNING) {
            return ResponseEntity.accepted()
                    .body(Map.of("status", "RUNNING", "message", "A job is already running  wait for it to finish first"));
        }
        service.deleteAll();
        service.autoMapAsync();
        return ResponseEntity.accepted()
                .body(Map.of("status", "STARTED", "message", "All mappings deleted. Full auto-map started."));
    }


    @GetMapping("/job-status")
    public ResponseEntity<Map<String, Object>> jobStatus() {
        JobResult job = service.getJobStatus();
        return ResponseEntity.ok(Map.of(
                "status",     job.status.name(),
                "message",    job.message,
                "counts",     job.counts,
                "startedAt",  job.startedAt  != null ? job.startedAt.toString()  : "",
                "finishedAt", job.finishedAt != null ? job.finishedAt.toString() : ""
        ));
    }


    @PatchMapping("/{mappingId}")
    public ResponseEntity<TickerSheetMapping> setManualMapping(
            @PathVariable String mappingId,
            @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(
                service.setManualMapping(mappingId, request.get("sheetName"), request.get("spreadsheetId"), request.get("gid"))
        );
    }

    @PostMapping("/bulk")
    public ResponseEntity<Void> updateBulkMappings(@RequestBody List<Map<String, Object>> requests) {
        service.updateBulkMappings(requests);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/map-all-100")
    public ResponseEntity<Map<String, String>> mapAllByThreshold(@RequestBody Map<String, Integer> request) {
        int threshold = request.getOrDefault("threshold", 100);
        service.mapByThresholdAsync(threshold);
        return ResponseEntity.accepted()
                .body(Map.of("status", "STARTED", "message", "Bulk mapping started for threshold " + threshold + "%."));
    }

    @GetMapping("/by-symbol/{symbol}")
    public ResponseEntity<TickerSheetMapping> getBySymbol(@PathVariable String symbol) {
        return service.getMappingBySymbol(symbol)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

package com.ash.tracker_service.service;

import com.ash.tracker_service.entity.Ticker;
import com.ash.tracker_service.entity.TickerSheetMapping;
import com.ash.tracker_service.entity.TickerSheetMapping.SheetCandidate;
import com.ash.tracker_service.repository.TickerRepository;
import com.ash.tracker_service.repository.TickerSheetMappingRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TickerSheetMappingService {

    private final TickerRepository tickerRepository;
    private final TickerSheetMappingRepository mappingRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${sheetnews.url:http://localhost:3005}")
    private String sheetNewsUrl;

    @Value("${sheetnews.api-key:secret123}")
    private String sheetNewsApiKey;


    public enum JobStatus { IDLE, RUNNING, DONE, FAILED }

    public static class JobResult {
        public volatile JobStatus status = JobStatus.IDLE;
        public volatile String message = "";
        public volatile Map<String, Integer> counts = Collections.emptyMap();
        public volatile Instant startedAt;
        public volatile Instant finishedAt;
    }

    private final JobResult currentJob = new JobResult();

    public JobResult getJobStatus() {
        return currentJob;
    }


    @Async
    public void autoMapAsync() {
        if (currentJob.status == JobStatus.RUNNING) {
            log.warn("autoMap already running, skipping duplicate request");
            return;
        }
        currentJob.status = JobStatus.RUNNING;
        currentJob.startedAt = Instant.now();
        currentJob.message = "Fetching sheet links from SheetNews";
        currentJob.counts = Collections.emptyMap();

        try {
            List<Map<String, String>> sheetLinks = fetchSheetLinks();
            if (sheetLinks.isEmpty()) {
                currentJob.status = JobStatus.FAILED;
                currentJob.message = "No sheet links returned  check SheetNews / x-api-key";
                return;
            }
            log.info("Fetched {} sheet names from SheetNews", sheetLinks.size());

            List<Ticker> tickers = loadUniqueTickers();
            log.info("Processing {} unique tickers", tickers.size());
            currentJob.message = "Mapping " + tickers.size() + " tickers against " + sheetLinks.size() + " sheets";

            int mapped = 0, ambiguous = 0, noMatch = 0, skipped = 0;

            for (Ticker ticker : tickers) {
                TickerSheetMapping existing = mappingRepository.findByIsin(ticker.getIsin()).orElse(null);


                if (existing != null && "MANUAL".equals(existing.getStatus())) {
                    skipped++;
                    continue;
                }

                List<SheetCandidate> candidates = findCandidates(ticker, sheetLinks);

                TickerSheetMapping mapping = existing != null ? existing : new TickerSheetMapping();
                mapping.setSymbol(ticker.getSymbol());
                mapping.setIsin(ticker.getIsin());
                mapping.setStockName(ticker.getName());
                mapping.setMappedAt(Instant.now());
                mapping.setCandidates(candidates);

                if (candidates.isEmpty()) {
                    mapping.setStatus("NO_MATCH");
                    mapping.setSheetName(null);
                    mapping.setSpreadsheetId(null);
                    mapping.setGid(null);
                    noMatch++;
                } else if (candidates.size() == 1) {
                    mapping.setStatus("MAPPED");
                    mapping.setSheetName(candidates.get(0).getSheetName());
                    mapping.setSpreadsheetId(candidates.get(0).getSpreadsheetId());
                    mapping.setGid(candidates.get(0).getGid());
                    mapped++;
                } else {

                    SheetCandidate best = candidates.get(0);
                    mapping.setStatus("AMBIGUOUS");
                    mapping.setSheetName(best.getSheetName());
                    mapping.setSpreadsheetId(best.getSpreadsheetId());
                    mapping.setGid(best.getGid());
                    ambiguous++;
                }

                mappingRepository.save(mapping);
            }

            Map<String, Integer> counts = Map.of(
                    "total", tickers.size(),
                    "mapped", mapped,
                    "ambiguous", ambiguous,
                    "noMatch", noMatch,
                    "skipped", skipped
            );
            currentJob.counts = counts;
            currentJob.status = JobStatus.DONE;
            currentJob.finishedAt = Instant.now();
            currentJob.message = String.format(
                    "Done! %d mapped, %d ambiguous, %d no-match, %d manual (skipped)",
                    mapped, ambiguous, noMatch, skipped
            );
            log.info("Ticker-centric auto-map complete: {}", counts);

        } catch (Exception e) {
            log.error("Auto-map failed", e);
            currentJob.status = JobStatus.FAILED;
            currentJob.message = "Error: " + e.getMessage();
            currentJob.finishedAt = Instant.now();
        }
    }


    public TickerSheetMapping setManualMapping(String mappingId, String sheetName, String spreadsheetId, String gid) {
        TickerSheetMapping.SheetCandidate candidate = TickerSheetMapping.SheetCandidate.builder()
                .sheetName(sheetName)
                .spreadsheetId(spreadsheetId)
                .gid(gid)
                .score(100)
                .build();
        return setManualMappings(mappingId, List.of(candidate));
    }


    public TickerSheetMapping setManualMappings(String mappingId, List<TickerSheetMapping.SheetCandidate> selectedSheets) {
        TickerSheetMapping mapping = mappingRepository.findById(mappingId)
                .orElseThrow(() -> new RuntimeException("Mapping not found: " + mappingId));
        
        if (selectedSheets == null || selectedSheets.isEmpty()) {
            mapping.setSheetName(null);
            mapping.setSpreadsheetId(null);
            mapping.setSelectedMappings(new ArrayList<>());
            mapping.setStatus("NO_MATCH");
        } else {

            TickerSheetMapping.SheetCandidate primary = selectedSheets.get(0);
            mapping.setSheetName(primary.getSheetName().trim());
            mapping.setSpreadsheetId(primary.getSpreadsheetId() != null ? primary.getSpreadsheetId().trim() : "");
            mapping.setGid(primary.getGid() != null ? primary.getGid().trim() : "");
            mapping.setSelectedMappings(selectedSheets);
            mapping.setStatus("MANUAL");
        }
        mapping.setMappedAt(Instant.now());
        return mappingRepository.save(mapping);
    }

    public void updateBulkMappings(List<Map<String, Object>> requests) {
        for (Map<String, Object> req : requests) {
            String id = (String) req.get("id");
            List<Map<String, Object>> selections = (List<Map<String, Object>>) req.get("selections");
            
            if (id != null) {
                try {
                    List<TickerSheetMapping.SheetCandidate> candidates = new ArrayList<>();
                    if (selections != null) {
                        for (Map<String, Object> sel : selections) {
                            candidates.add(TickerSheetMapping.SheetCandidate.builder()
                                    .sheetName((String) sel.get("sheetName"))
                                    .spreadsheetId((String) sel.get("spreadsheetId"))
                                    .gid((String) sel.get("gid"))
                                    .score(sel.containsKey("score") ? (Integer) sel.get("score") : 100)
                                    .build());
                        }
                    }
                    setManualMappings(id, candidates);
                } catch (Exception e) {
                    log.error("Bulk mapping failed for ID {}: {}", id, e.getMessage());
                }
            }
        }
    }

    @Async
    public void mapByThresholdAsync(int threshold) {
        if (currentJob.status == JobStatus.RUNNING) {
            log.warn("Another job already running, skipping mapByThreshold request");
            return;
        }

        currentJob.status = JobStatus.RUNNING;
        currentJob.startedAt = Instant.now();
        currentJob.message = "Starting bulk mapping with threshold " + threshold + "%";
        currentJob.counts = Collections.emptyMap();

        try {
            List<TickerSheetMapping> unconfirmed = mappingRepository.findAll().stream()
                    .filter(m -> !"MAPPED".equals(m.getStatus()) && !"MANUAL".equals(m.getStatus()))
                    .collect(Collectors.toList());

            long count = 0;
            int total = unconfirmed.size();
            int processed = 0;

            for (TickerSheetMapping m : unconfirmed) {
                if (m.getCandidates() != null) {
                    List<TickerSheetMapping.SheetCandidate> validMatches = m.getCandidates().stream()
                            .filter(c -> c.getScore() >= threshold)
                            .collect(Collectors.toList());

                    if (!validMatches.isEmpty()) {
                        setManualMappings(m.getId(), validMatches);
                        count++;
                    }
                }
                
                processed++;
                if (processed % 50 == 0) {
                    currentJob.message = String.format("Processing: %d/%d (mapped %d so far)", processed, total, count);
                }
            }

            currentJob.counts = Map.of("mappedCount", (int)count, "processed", total);
            currentJob.status = JobStatus.DONE;
            currentJob.finishedAt = Instant.now();
            currentJob.message = String.format("Bulk mapping complete! Auto-mapped %d stocks with >= %d%% confidence.", count, threshold);
            log.info("Bulk auto-map complete: {}/{}", count, total);

        } catch (Exception e) {
            log.error("Bulk mapping failed", e);
            currentJob.status = JobStatus.FAILED;
            currentJob.message = "Error: " + e.getMessage();
            currentJob.finishedAt = Instant.now();
        }
    }

    public List<Map<String, Object>> getAllEnriched() {
        List<TickerSheetMapping> mappings = mappingRepository.findAllByOrderByStatusAscSymbolAsc();
        List<Ticker> allTickers = tickerRepository.findAll();
        

        Map<String, Long> viewCounts = allTickers.stream()
                .filter(t -> t.getIsin() != null)
                .collect(Collectors.toMap(
                        Ticker::getIsin,
                        t -> t.getViewCount() != null ? t.getViewCount() : 0L,
(a, b) -> a 
                ));

        return mappings.stream()
                .filter(m -> m.getIsin() != null)
                .collect(Collectors.toMap(
                        TickerSheetMapping::getIsin,
                        m -> m,
(existing, replacement) -> existing 
                ))
                .values().stream()
                .map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", m.getId());
                    map.put("symbol", m.getSymbol());
                    map.put("isin", m.getIsin());
                    map.put("stockName", m.getStockName());
                    map.put("sheetName", m.getSheetName());
                    map.put("spreadsheetId", m.getSpreadsheetId());
                    map.put("gid", m.getGid());
                    map.put("status", m.getStatus());
                    map.put("candidates", m.getCandidates());
                    map.put("mappedAt", m.getMappedAt());
                    map.put("viewCount", viewCounts.getOrDefault(m.getIsin(), 0L));
                    return map;
                }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getUnmappedSortedByViewCount() {
        return getAllEnriched().stream()
                .filter(m -> {
                    String status = (String) m.get("status");
                    return "AMBIGUOUS".equals(status) || "NO_MATCH".equals(status);
                })
                .sorted((a, b) -> {
                    Long v1 = (Long) a.getOrDefault("viewCount", 0L);
                    Long v2 = (Long) b.getOrDefault("viewCount", 0L);
return v2.compareTo(v1); 
                })
                .collect(Collectors.toList());
    }

    public List<TickerSheetMapping> getByStatus(String status) {
        return mappingRepository.findByStatus(status.toUpperCase());
    }

    public long deleteAll() {
        long count = mappingRepository.count();
        mappingRepository.deleteAll();
        log.info("Deleted all {} ticker-sheet mappings", count);
        return count;
    }


    @Async
    public void refreshAsync() {
        if (currentJob.status == JobStatus.RUNNING) {
            log.warn("refreshAsync: job already running, skipping");
            return;
        }
        currentJob.status = JobStatus.RUNNING;
        currentJob.startedAt = Instant.now();
        currentJob.message = "Fetching sheet links for refresh";
        currentJob.counts = Collections.emptyMap();

        try {
            List<Map<String, String>> sheetLinks = fetchSheetLinks();
            if (sheetLinks.isEmpty()) {
                currentJob.status = JobStatus.FAILED;
                currentJob.message = "No sheet links returned  check SheetNews connection";
                return;
            }

            List<Ticker> allTickers = loadUniqueTickers();

            List<Ticker> unmappedTickers = allTickers.stream()
                    .filter(t -> t.getIsin() != null)
                    .filter(t -> mappingRepository.findByIsin(t.getIsin()).isEmpty())
                    .collect(Collectors.toList());

            log.info("Refresh: {} new tickers to map (out of {} total)", unmappedTickers.size(), allTickers.size());
            currentJob.message = "Mapping " + unmappedTickers.size() + " new tickers";

            int mapped = 0, ambiguous = 0, noMatch = 0;
            for (Ticker ticker : unmappedTickers) {
                List<SheetCandidate> candidates = findCandidates(ticker, sheetLinks);

                TickerSheetMapping mapping = new TickerSheetMapping();
                mapping.setSymbol(ticker.getSymbol());
                mapping.setIsin(ticker.getIsin());
                mapping.setStockName(ticker.getName());
                mapping.setMappedAt(Instant.now());
                mapping.setCandidates(candidates);

                if (candidates.isEmpty()) {
                    mapping.setStatus("NO_MATCH");
                    noMatch++;
                } else if (candidates.size() == 1) {
                    mapping.setStatus("MAPPED");
                    mapping.setSheetName(candidates.get(0).getSheetName());
                    mapping.setSpreadsheetId(candidates.get(0).getSpreadsheetId());
                    mapping.setGid(candidates.get(0).getGid());
                    mapped++;
                } else {
                    SheetCandidate best = candidates.get(0);
                    mapping.setStatus("AMBIGUOUS");
                    mapping.setSheetName(best.getSheetName());
                    mapping.setSpreadsheetId(best.getSpreadsheetId());
                    mapping.setGid(best.getGid());
                    ambiguous++;
                }
                mappingRepository.save(mapping);
            }

            currentJob.counts = Map.of(
                    "new", unmappedTickers.size(), "mapped", mapped,
                    "ambiguous", ambiguous, "noMatch", noMatch);
            currentJob.status = JobStatus.DONE;
            currentJob.finishedAt = Instant.now();
            currentJob.message = String.format(
                    "Refresh done! %d new  %d mapped, %d ambiguous, %d no-match",
                    unmappedTickers.size(), mapped, ambiguous, noMatch);
            log.info("Refresh complete: {}", currentJob.counts);

        } catch (Exception e) {
            log.error("Refresh failed", e);
            currentJob.status = JobStatus.FAILED;
            currentJob.message = "Error: " + e.getMessage();
            currentJob.finishedAt = Instant.now();
        }
    }



    public List<Map<String, String>> getAvailableSheets() {
        return fetchSheetLinks();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> fetchSheetLinks() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", sheetNewsApiKey);
            String url = sheetNewsUrl + "/api/stock-links";
            log.info("Fetching sheet links from: {}", url);
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );
            log.info("SheetNews response for links: {}", response.getStatusCode());
            Object data = Objects.requireNonNull(response.getBody()).get("data");
            return objectMapper.convertValue(data, new TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            log.error("Failed to fetch sheet links from {}: {}", sheetNewsUrl, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Ticker> loadUniqueTickers() {
        List<Ticker> all = tickerRepository.findAll();
        Map<String, Ticker> byIsin = new LinkedHashMap<>();
        for (Ticker t : all) {
            String isin = t.getIsin();
            if (isin == null || isin.isBlank()) continue;
            Ticker existing = byIsin.get(isin);
            if (existing == null || sourceRank(t) < sourceRank(existing)) {
                byIsin.put(isin, t);
            }
        }
        return new ArrayList<>(byIsin.values());
    }

    private int sourceRank(Ticker t) {
        return switch (t.getSource() != null ? t.getSource() : "") {
            case "NSE" -> 0;
            case "BSE" -> 1;
            default -> 2;
        };
    }

    private List<SheetCandidate> findCandidates(Ticker ticker, List<Map<String, String>> sheets) {
        String tickerName = ticker.getName() != null ? ticker.getName().toLowerCase().trim() : "";
        String symbol = ticker.getSymbol() != null ? ticker.getSymbol().toLowerCase().trim() : "";

        List<SheetCandidate> matches = new ArrayList<>();
        for (Map<String, String> sheet : sheets) {
            String sheetName = sheet.get("name");
            String spreadsheetId = sheet.get("spreadsheetId");
            if (sheetName == null) continue;
            int score = scoreMatch(tickerName, symbol, sheetName.toLowerCase().trim());
            if (score > 0) {
                matches.add(SheetCandidate.builder()
                        .sheetName(sheetName)
                        .spreadsheetId(spreadsheetId)
                        .gid(sheet.get("gid"))
                        .score(score)
                        .build());
            }
        }
        matches.sort(Comparator.comparingInt(SheetCandidate::getScore).reversed());
        return matches.stream().limit(5).collect(Collectors.toList());
    }



    private static final Set<String> STOP_WORDS = Set.of(
            "limited", "ltd",
            "industries", "industry",
            "enterprises", "enterprise",
            "corporation", "corp",
            "international", "infotech", "infosystems", "solutions",
            "technologies", "technology", "tech",
            "services", "service",
            "holdings", "holding",
            "investments", "investment",
            "group", "global",
            "india", "indian",
            "finance", "financial",
            "trading", "traders",
            "products", "manufacturing",
            "chemicals", "pharma",
            "motors", "auto",
            "bank", "banks",
            "energy", "power",
            "realty", "real",
            "works", "systems", "infra"
    );

    private String stripStopWords(String name) {
        if (name == null) return "";
        String[] tokens = name.toLowerCase().split("[\\s\\-&.()/,]+");
        StringBuilder sb = new StringBuilder();
        for (String t : tokens) {
            if (!STOP_WORDS.contains(t) && !t.isBlank()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(t);
            }
        }
        return sb.toString().trim();
    }


    private int scoreMatch(String tickerNameRaw, String symbolRaw, String sheetNameLower) {
        if (tickerNameRaw == null || tickerNameRaw.isEmpty()) return 0;
        
        String tickerName = tickerNameRaw.toLowerCase().trim();
        String symbol = symbolRaw != null ? symbolRaw.toLowerCase().trim() : "";

        if (tickerName.equals(sheetNameLower)) return 100;

        String strippedTicker = stripStopWords(tickerName);
        String strippedSheet  = stripStopWords(sheetNameLower);

        if (strippedTicker.isEmpty() || strippedSheet.isEmpty()) return 0;

        if (strippedTicker.equals(strippedSheet))       return 70;
        if (strippedSheet.contains(strippedTicker))     return 60;
        if (strippedTicker.contains(strippedSheet))     return 50;

        if (!symbol.isEmpty() && sheetNameLower.contains(symbol)) return 30;

        String[] tickerWords = strippedTicker.split("[\\s\\-&.()/,]+");
        for (String w : tickerWords) {
            if (w.length() >= 5 && !STOP_WORDS.contains(w) && strippedSheet.contains(w)) return 15;
        }
        String[] sheetWords = strippedSheet.split("[\\s\\-&.()/,]+");
        for (String w : sheetWords) {
            if (w.length() >= 5 && !STOP_WORDS.contains(w) && strippedTicker.contains(w)) return 10;
        }

        return 0;
    }

    public java.util.Optional<TickerSheetMapping> getMappingBySymbol(String identifier) {
        if (identifier == null || identifier.isEmpty()) return java.util.Optional.empty();

        java.util.Optional<TickerSheetMapping> byIsin = mappingRepository.findByIsin(identifier);
        if (byIsin.isPresent()) return byIsin;

        java.util.Optional<TickerSheetMapping> bySymbol = mappingRepository.findBySymbolIgnoreCase(identifier);
        if (bySymbol.isPresent()) return bySymbol;

        if (!identifier.contains(".")) {
            java.util.Optional<TickerSheetMapping> ns = mappingRepository.findBySymbolIgnoreCase(identifier + ".NS");
            if (ns.isPresent()) return ns;
            java.util.Optional<TickerSheetMapping> bo = mappingRepository.findBySymbolIgnoreCase(identifier + ".BO");
            if (bo.isPresent()) return bo;
        } else {
            String base = identifier.split("\\.")[0];
            java.util.Optional<TickerSheetMapping> baseOpt = mappingRepository.findBySymbolIgnoreCase(base);
            if (baseOpt.isPresent()) return baseOpt;
        }

        return java.util.Optional.empty();
    }
}

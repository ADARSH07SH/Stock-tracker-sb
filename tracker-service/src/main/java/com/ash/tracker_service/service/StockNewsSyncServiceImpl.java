package com.ash.tracker_service.service;

import com.ash.tracker_service.entity.StockNewsItem;
import com.ash.tracker_service.entity.Ticker;
import com.ash.tracker_service.entity.TickerSheetMapping;
import com.ash.tracker_service.repository.StockNewsRepository;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockNewsSyncServiceImpl implements StockNewsSyncService {

    private final StockNewsRepository stockNewsRepository;
    private final TickerRepository tickerRepository;
    private final TickerSheetMappingRepository mappingRepository;
    private final TickerSheetMappingService mappingService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration CACHE_TTL = Duration.ofDays(1);

    @Value("${sheetnews.url:http://localhost:3005}")
    private String sheetNewsUrl;

    @Value("${sheetnews.api-key:secret123}")
    private String sheetNewsApiKey;



    @Override
    public int syncAll() {
        log.info("Starting full stock news sync...");

        List<Map<String, String>> stockLinks = fetchStockLinks();
        if (stockLinks.isEmpty()) {
            log.warn("No stock links returned from SheetNews");
            return 0;
        }

        List<Ticker> allTickers = tickerRepository.findAll();
        log.info("Fetched {} stock links from SheetNews", stockLinks.size());
        int syncedCount = 0;

        for (Ticker ticker : allTickers) {
            String symbol = ticker.getSymbol();
            String stockName = ticker.getName();

            try {
                TickerSheetMapping mapping = mappingService.getMappingBySymbol(symbol).orElse(null);
                
                List<Map<String, String>> rows = new ArrayList<>();
                String effectiveSheetName = symbol;
                String effectiveSpreadsheetId = null;

                if (mapping != null && mapping.getSelectedMappings() != null && !mapping.getSelectedMappings().isEmpty()) {
                    effectiveSpreadsheetId = mapping.getSpreadsheetId();
                    effectiveSheetName = mapping.getSheetName();
                    
                    for (TickerSheetMapping.SheetCandidate sel : mapping.getSelectedMappings()) {
                        if (sel.getSpreadsheetId() != null && !sel.getSpreadsheetId().isBlank() && sel.getGid() != null && !sel.getGid().isBlank()) {
                            rows.addAll(fetchNewsBySpreadsheetId(sel.getSpreadsheetId(), sel.getGid()));
                        } else if (sel.getSheetName() != null && !sel.getSheetName().isBlank()) {
                            rows.addAll(fetchNewsRows(sel.getSheetName()));
                        } else {
                            rows.addAll(fetchNewsRows(mapping.getSheetName() != null ? mapping.getSheetName() : stockName));
                        }
                    }
                } else if (mapping != null && mapping.getSpreadsheetId() != null && !mapping.getSpreadsheetId().isBlank() && mapping.getGid() != null && !mapping.getGid().isBlank()) {
                    effectiveSpreadsheetId = mapping.getSpreadsheetId();
                    effectiveSheetName = mapping.getSheetName();
                    rows = fetchNewsBySpreadsheetId(effectiveSpreadsheetId, mapping.getGid());
                } else {

                    String lookupName = (mapping != null && mapping.getSheetName() != null && !mapping.getSheetName().isBlank())
                            ? mapping.getSheetName()
                            : (mapping != null && mapping.getStockName() != null && !mapping.getStockName().isBlank())
                            ? mapping.getStockName()
                            : stockName;
                    effectiveSheetName = lookupName;
                    rows = fetchNewsRows(lookupName);
                }


                String lookupSymbol = (mapping != null && mapping.getSymbol() != null && !mapping.getSymbol().isBlank())
                        ? mapping.getSymbol() : symbol;

                StockNewsItem item = stockNewsRepository.findBySymbolIgnoreCase(lookupSymbol)
                        .or(() -> mapping != null ? stockNewsRepository.findByIsinIgnoreCase(mapping.getIsin()) : Optional.empty())
                        .orElse(StockNewsItem.builder().symbol(lookupSymbol).build());

                item.setSheetName(effectiveSheetName);
                item.setSpreadsheetId(effectiveSpreadsheetId);
                item.setGid(mapping != null ? mapping.getGid() : null);
                item.setStockName(mapping != null ? mapping.getStockName() : stockName);
                item.setSymbol(symbol);
                item.setIsin(mapping != null ? mapping.getIsin() : ticker.getIsin());
                item.setRows(rows);
                item.setSyncedAt(Instant.now());

                stockNewsRepository.save(item);
                syncedCount++;

                log.info("Synced: {} mappings  {} ({})", 
                        (mapping != null && mapping.getSelectedMappings() != null) ? mapping.getSelectedMappings().size() : 1,
                        item.getStockName(), item.getSymbol());

            } catch (Exception e) {
                log.warn("Failed to sync ticker '{}': {}", symbol, e.toString());
            }
        }

        log.info("Sync complete. {}/{} stocks synced.", syncedCount, stockLinks.size());
        return syncedCount;
    }



    @Override
    public List<StockNewsItem> getAll() {
        return stockNewsRepository.findAllByOrderBySyncedAtDesc();
    }

    @Override
    public StockNewsItem getBySymbol(String symbol) {
        Optional<StockNewsItem> bySymbol = stockNewsRepository.findBySymbolIgnoreCase(symbol);
        if (bySymbol.isPresent()) return bySymbol.get();

        Optional<StockNewsItem> byIsin = tickerRepository.findAll().stream()
                .filter(t -> symbol.equalsIgnoreCase(t.getSymbol()) && t.getIsin() != null)
                .findFirst()
                .flatMap(t -> stockNewsRepository.findByIsinIgnoreCase(t.getIsin()));
        if (byIsin.isPresent()) return byIsin.get();

        return stockNewsRepository.findByIsinIgnoreCase(symbol).orElse(null);
    }

    @Override
    public List<StockNewsItem> searchByName(String name) {
        return stockNewsRepository.findByStockNameContainingIgnoreCase(name);
    }

    @Override
    public List<StockNewsItem> searchNews(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        String[] keywords = query.toLowerCase().split("\\s+");

        return stockNewsRepository.findAll().stream()
                .filter(item -> {
                    for (String kw : keywords) {
                        boolean matches = false;
                        if (item.getStockName() != null && item.getStockName().toLowerCase().contains(kw)) matches = true;
                        if (!matches && item.getSymbol() != null && item.getSymbol().toLowerCase().contains(kw)) matches = true;
                        if (!matches && item.getIsin() != null && item.getIsin().toLowerCase().contains(kw)) matches = true;
                        if (!matches && item.getRows() != null) {
                            for (Map<String, String> row : item.getRows()) {
                                if (row.values().stream().anyMatch(val -> val != null && val.toLowerCase().contains(kw))) {
                                    matches = true;
                                    break;
                                }
                            }
                        }
                        if (!matches) return false;
                    }
                    return true;
                })
                .map(item -> StockNewsItem.builder()
                        .id(item.getId())
                        .stockName(item.getStockName())
                        .symbol(item.getSymbol())
                        .isin(item.getIsin())
                        .sheetName(item.getSheetName())
                        .spreadsheetId(item.getSpreadsheetId())
                        .gid(item.getGid())
                        .syncedAt(item.getSyncedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getAvailableStockLinks() {
        List<Map<String, String>> links = fetchStockLinks();
        return Map.of("status", "success", "data", links);
    }

    @Override
    public Map<String, Object> getRawSheetNews(String stockName) {
        try {
            String encodedName = URLEncoder.encode(stockName, StandardCharsets.UTF_8);
            String url = sheetNewsUrl + "/api/sheet-news/" + encodedName;
            log.info("Proxying raw news for [{}] from: {}", stockName, url);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(apiKeyHeaders()), Map.class
            );
            
            Map<String, Object> body = response.getBody();
            if (body != null && "success".equals(body.get("status"))) {
                Map<String, Object> data = (Map<String, Object>) body.get("data");
                if (data != null) {
                    
                    data.remove("url");
                }
            }
            return body;
        } catch (Exception e) {
            log.warn("Failed to proxy news for '{}': {}", stockName, e.toString());
            return Map.of("status", "error", "message", "Failed to fetch news from SheetNews");
        }
    }



    @Override
    @Async
    public void refreshIfStaleAsync(String symbol) {
        try {
            StockNewsItem cached = stockNewsRepository.findBySymbolIgnoreCase(symbol).orElse(null);

            if (cached != null && cached.getSyncedAt() != null) {
                Duration age = Duration.between(cached.getSyncedAt(), Instant.now());
                boolean hasRows = cached.getRows() != null && !cached.getRows().isEmpty();

                if (age.compareTo(CACHE_TTL) < 0 && hasRows) {
                    log.info("Cache fresh for {} (age: {}h, rows: {}), skipping refresh", symbol, age.toHours(), cached.getRows().size());
                    return;
                }
            }

            TickerSheetMapping mapping = mappingService.getMappingBySymbol(symbol).orElse(null);
            
            List<Map<String, String>> rows = new ArrayList<>();
            String effectiveSheetName = symbol;
            String effectiveSpreadsheetId = null;

            if (mapping != null && mapping.getSelectedMappings() != null && !mapping.getSelectedMappings().isEmpty()) {
                effectiveSpreadsheetId = mapping.getSpreadsheetId();
                effectiveSheetName = mapping.getSheetName();
                for (TickerSheetMapping.SheetCandidate sel : mapping.getSelectedMappings()) {
                    if (sel.getSpreadsheetId() != null && !sel.getSpreadsheetId().isBlank() && sel.getGid() != null && !sel.getGid().isBlank()) {
                        rows.addAll(fetchNewsBySpreadsheetId(sel.getSpreadsheetId(), sel.getGid()));
                    } else if (sel.getSheetName() != null && !sel.getSheetName().isBlank()) {
                        rows.addAll(fetchNewsRows(sel.getSheetName()));
                    } else {
                        rows.addAll(fetchNewsRows(mapping.getSheetName() != null ? mapping.getSheetName() : symbol));
                    }
                }
            } else if (mapping != null && mapping.getSpreadsheetId() != null && !mapping.getSpreadsheetId().isBlank() && mapping.getGid() != null && !mapping.getGid().isBlank()) {
                effectiveSpreadsheetId = mapping.getSpreadsheetId();
                effectiveSheetName = mapping.getSheetName();
                rows = fetchNewsBySpreadsheetId(effectiveSpreadsheetId, mapping.getGid());
            } else {
                String lookupName = (mapping != null && mapping.getSheetName() != null && !mapping.getSheetName().isBlank())
                        ? mapping.getSheetName()
                        : (mapping != null && mapping.getStockName() != null && !mapping.getStockName().isBlank())
                        ? mapping.getStockName()
                        : symbol;
                effectiveSheetName = lookupName;
                rows = fetchNewsRows(lookupName);
            }

            Ticker resolvedTicker = tickerRepository.findAll().stream()
                    .filter(t -> symbol.equalsIgnoreCase(t.getSymbol()) && t.getIsin() != null)
                    .findFirst().orElse(null);
            String resolvedIsin = resolvedTicker != null ? resolvedTicker.getIsin() : null;

            StockNewsItem item = cached != null ? cached
                    : stockNewsRepository.findBySymbolIgnoreCase(symbol)
                            .or(() -> resolvedIsin != null ? stockNewsRepository.findByIsinIgnoreCase(resolvedIsin) : Optional.empty())
                            .orElse(StockNewsItem.builder().symbol(symbol).build());

            item.setSheetName(effectiveSheetName);
            item.setSpreadsheetId(effectiveSpreadsheetId);
item.setGid(mapping != null ? mapping.getGid() : (item.getGid())); 
            item.setSymbol(symbol);
            item.setIsin(resolvedIsin != null ? resolvedIsin : (mapping != null ? mapping.getIsin() : null));
            item.setStockName(mapping != null ? mapping.getStockName() : (resolvedTicker != null ? resolvedTicker.getName() : symbol));
            item.setRows(rows);
            item.setSyncedAt(Instant.now());

            stockNewsRepository.save(item);
            log.info("Refreshed news for {} (ISIN: {}) - {} rows", symbol, item.getIsin(), rows.size());

        } catch (Exception e) {
            log.warn("Failed to refresh stale news for {}: {}", symbol, e.toString());
        }
    }



    private HttpHeaders apiKeyHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", sheetNewsApiKey);
        return headers;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> fetchStockLinks() {
        try {
            String url = sheetNewsUrl + "/api/stock-links";
            log.info("Fetching stock links from: {}", url);
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(apiKeyHeaders()), Map.class
            );
            log.info("SheetNews response for links: {}", response.getStatusCode());
            Object data = Objects.requireNonNull(response.getBody()).get("data");
            return objectMapper.convertValue(data, new TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            log.error("Failed to fetch stock links: {}", e.toString());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> fetchNewsRows(String stockName) {
        try {
            String encodedName = URLEncoder.encode(stockName, StandardCharsets.UTF_8);
            String url = sheetNewsUrl + "/api/sheet-news/" + encodedName;
            log.info("Fetching news for [{}] from: {}", stockName, url);
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(apiKeyHeaders()), Map.class
            );
            log.info("SheetNews response for [{}]: {}", stockName, response.getStatusCode());
            Map<String, Object> body = response.getBody();
            if (body == null) {
                log.warn("SheetNews returned null body for [{}]", stockName);
                return Collections.emptyList();
            }
            
            log.info("SheetNews response keys for [{}]: {}", stockName, body.keySet());
            
            Map<String, Object> data = (Map<String, Object>) body.get("data");
            if (data == null) {
                log.warn("SheetNews body contains no 'data' field for [{}]", stockName);
                return Collections.emptyList();
            }
            
            Object rows = data.get("data");
            if (rows == null) {
                log.warn("SheetNews 'data' field contains no nested 'data' (rows) for [{}]", stockName);
                return Collections.emptyList();
            }

            List<Map<String, String>> result = objectMapper.convertValue(rows, new TypeReference<List<Map<String, String>>>() {});
            log.info("Fetched {} rows successfully for [{}]", result.size(), stockName);
            if (!result.isEmpty()) {
                log.info("First row for [{}]: {}", stockName, result.get(0));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch news rows for '{}': {}", stockName, e.toString());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> fetchNewsBySpreadsheetId(String spreadsheetId, String gid) {
        try {
            String url = sheetNewsUrl + "/api/spreadsheet-news/" + spreadsheetId;
            if (gid != null && !gid.isBlank()) {
                url += "?gid=" + gid;
            }
            log.info("Deterministic fetching news for ID [{}] (GID: {}) from: {}", spreadsheetId, gid, url);
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(apiKeyHeaders()), Map.class
            );
            log.info("SheetNews response for ID [{}]: {}", spreadsheetId, response.getStatusCode());
            Map<String, Object> body = response.getBody();
            if (body == null) return Collections.emptyList();

            Map<String, Object> data = (Map<String, Object>) body.get("data");
            if (data == null) return Collections.emptyList();

            Object rows = data.get("data");
            if (rows == null) return Collections.emptyList();

            List<Map<String, String>> result = objectMapper.convertValue(rows, new TypeReference<List<Map<String, String>>>() {});
            log.info("Fetched {} rows successfully for ID [{}]", result.size(), spreadsheetId);
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch news by ID '{}': {}", spreadsheetId, e.getMessage());
            return Collections.emptyList();
        }
    }
}

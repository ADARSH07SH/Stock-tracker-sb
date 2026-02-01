package com.ash.tracker_service.service;

import com.ash.tracker_service.entity.MarketPrice;
import com.ash.tracker_service.repository.MarketPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarketPriceService {

    private final MarketPriceRepository marketPriceRepository;
    private final ExternalPriceClient externalPriceClient;

    private static final long TTL = 300;

    public Map<String, Double> getLatestPrices(List<String> isins) {

        Map<String, Double> result = new HashMap<>();

        List<MarketPrice> cached = marketPriceRepository.findByIsinIn(isins);
        Map<String, MarketPrice> cacheMap = cached.stream()
                .collect(Collectors.toMap(MarketPrice::getIsin, p -> p));

        List<String> fetch = new ArrayList<>();

        for (String isin : isins) {
            MarketPrice p = cacheMap.get(isin);
            if (p == null || p.getLastUpdated().isBefore(Instant.now().minusSeconds(TTL))) {
                fetch.add(isin);
            } else {
                result.put(isin, p.getPrice());
            }
        }

        if (!fetch.isEmpty()) {
            Map<String, Double> fetched = externalPriceClient.fetchPrices(fetch);
            for (var e : fetched.entrySet()) {
                result.put(e.getKey(), e.getValue());
                marketPriceRepository.save(
                        MarketPrice.builder()
                                .isin(e.getKey())
                                .price(e.getValue())
                                .lastUpdated(Instant.now())
                                .build()
                );
            }
        }

        return result;
    }
}

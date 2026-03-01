package com.ash.tracker_service.service;

import com.ash.tracker_service.entity.MarketPrice;
import com.ash.tracker_service.repository.MarketPriceRepository;
import com.ash.tracker_service.repository.MissingIsinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ExternalPriceClientImpl implements ExternalPriceClient {

    private final MarketPriceRepository marketPriceRepository;
    private final YahooMarketService yahooMarketService;
    private final TickerSearchService tickerSearchService;
    private final MissingIsinRepository missingIsinRepository;

    private static final long TTL_SECONDS = 600;

    @Override
    public Map<String, Double> fetchPrices(List<String> isins) {

        Map<String, Double> prices = new HashMap<>();
        List<MarketPrice> cached = marketPriceRepository.findByIsinIn(isins);

        Map<String, MarketPrice> cacheMap = new HashMap<>();
        for (MarketPrice p : cached) {
            cacheMap.put(p.getIsin(), p);
        }

        for (String isin : isins) {
            try {
                MarketPrice cachedPrice = cacheMap.get(isin);

                if (cachedPrice != null &&
                        cachedPrice.getLastUpdated().isAfter(
                                Instant.now().minusSeconds(TTL_SECONDS))) {

                    prices.put(isin, cachedPrice.getPrice());
                    continue;
                }

                String symbol = null;
                
                var missingIsin = missingIsinRepository.findByIsin(isin);
                if (missingIsin.isPresent() && missingIsin.get().getSymbol() != null && !missingIsin.get().getSymbol().isEmpty()) {
                    symbol = missingIsin.get().getSymbol();
                    System.out.println("Using symbol from missing_isins: " + symbol + " for ISIN: " + isin);
                } else {
                    symbol = tickerSearchService.getSymbolByIsin(isin);
                }
                
                double livePrice = yahooMarketService.getCurrentPrice(symbol);

                prices.put(isin, livePrice);

                marketPriceRepository.save(
                        MarketPrice.builder()
                                .isin(isin)
                                .price(livePrice)
                                .lastUpdated(Instant.now())
                                .build()
                );
            } catch (Exception e) {
                System.err.println(" Failed to fetch price for ISIN: " + isin + " - " + e.getMessage());
               
            }
        }

        return prices;
    }
}

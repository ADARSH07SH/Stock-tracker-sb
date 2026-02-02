package com.ash.tracker_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class YahooMarketService {

    @Value("${yahoo.base-url}")
    private String yahooBaseUrl;

    private final RestTemplate restTemplate;

    private HttpEntity<Void> buildEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36"
        );
        return new HttpEntity<>(headers);
    }

    private String buildYahooSymbol(String symbol, String exchange) {
        if (symbol.contains(".")) return symbol.toUpperCase();
        return symbol.toUpperCase() + exchange;
    }

    public Object getChartAndQuote(String symbol, String interval, String range) {
     
        try {
            String yahooSymbol = buildYahooSymbol(symbol, ".NS");
            String url = yahooBaseUrl +
                    "/v8/finance/chart/" + yahooSymbol +
                    "?interval=" + interval +
                    "&range=" + range;

            return restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    buildEntity(),
                    Object.class
            ).getBody();
        } catch (Exception e) {
            // If NSE fails, try BSE (.BS)
            System.out.println("NSE failed for " + symbol + ", trying BSE");
            String yahooSymbol = buildYahooSymbol(symbol, ".BO");
            String url = yahooBaseUrl +
                    "/v8/finance/chart/" + yahooSymbol +
                    "?interval=" + interval +
                    "&range=" + range;

            return restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    buildEntity(),
                    Object.class
            ).getBody();
        }
    }

    @SuppressWarnings("unchecked")
    public double getCurrentPrice(String symbol) {
        System.out.println("Fetching price for symbol: " + symbol);

        Object response = getChartAndQuote(symbol, "1d", "1d");

        Map<String, Object> root = (Map<String, Object>) response;
        Map<String, Object> chart = (Map<String, Object>) root.get("chart");

        List<Map<String, Object>> result =
                (List<Map<String, Object>>) chart.get("result");

        if (result == null || result.isEmpty()) {
            throw new RuntimeException("Yahoo price not available");
        }

        Map<String, Object> meta =
                (Map<String, Object>) result.get(0).get("meta");

        Object price = meta.get("regularMarketPrice");

        if (price == null) {
            throw new RuntimeException("regularMarketPrice missing");
        }

        return ((Number) price).doubleValue();
    }
}

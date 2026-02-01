package com.ash.tracker_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Service
@RequiredArgsConstructor
public class FyersMarketService {
    @Value("${fyers.base-url}")
    private String fyersBaseUrl;

    @Value("${fyers.api-key}")
    private String fyersApiKey;

    private final RestTemplate restTemplate;

    private HttpEntity<Void> buildEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", fyersApiKey);
        return new HttpEntity<>(headers);
    }

    private String buildFyersSymbol(String symbol) {
        return "NSE:" + symbol.toUpperCase() + "-EQ";
    }

    public Object getQuote(String symbol) {
        String fyersSymbol =symbol;
        String url = fyersBaseUrl + "/stockData/" + fyersSymbol;

        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                buildEntity(),
                Object.class
        ).getBody();
    }

    public Object getChart(String symbol, String resolution, long from, long to) {
        String fyersSymbol = buildFyersSymbol(symbol);

        String url = fyersBaseUrl + "/getChart" +
                "?symbol=" + fyersSymbol +
                "&resolution=" + resolution +
                "&range_from=" + from +
                "&range_to=" + to;

        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                buildEntity(),
                Object.class
        ).getBody();
    }
}

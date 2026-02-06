package com.ash.tracker_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
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
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return new HttpEntity<>(headers);
    }

    private String buildFyersSymbol(String symbol) {
        String upper = symbol.toUpperCase();

        if (upper.startsWith("NSE:") && upper.endsWith("-EQ")) {
            return upper;
        }
        if (upper.startsWith("NSE:")) {
            return upper + "-EQ";
        }
        return "NSE:" + upper + "-EQ";
    }

    public Object getQuote(String symbol) {
        try {
            String fyersSymbol = buildFyersSymbol(symbol);
            String encodedSymbol = URLEncoder.encode(fyersSymbol, StandardCharsets.UTF_8);
            String url = fyersBaseUrl + "/stockData/" + encodedSymbol;

            ResponseEntity<Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    buildEntity(),
                    Object.class
            );

            return response.getBody();

        } catch (HttpClientErrorException e) {
            log.error("Client error fetching quote for {}: {} - {}",
                    symbol, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Quote fetch failed: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (HttpServerErrorException e) {
            log.error("Server error fetching quote for {}: {} - {}",
                    symbol, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Quote fetch failed: Server error " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            log.error("Network error fetching quote for {}: {}", symbol, e.getMessage(), e);
            throw new RuntimeException("Quote fetch failed: Network error - " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error fetching quote for {}: {}", symbol, e.getMessage(), e);
            throw new RuntimeException("Quote fetch failed: " + e.getMessage(), e);
        }
    }

    public Object getChart(String symbol, String resolution, long from, long to) {
        try {
            String fyersSymbol = buildFyersSymbol(symbol);

            String url = UriComponentsBuilder.fromHttpUrl(fyersBaseUrl + "/getChart")
                    .queryParam("symbol", fyersSymbol)
                    .queryParam("resolution", resolution)
                    .queryParam("range_from", from)
                    .queryParam("range_to", to)
                    .build()
                    .toUriString();

            ResponseEntity<Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    buildEntity(),
                    Object.class
            );

            return response.getBody();

        } catch (HttpClientErrorException e) {
            log.error("Client error fetching chart for {}: {} - {}",
                    symbol, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Chart fetch failed: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (HttpServerErrorException e) {
            log.error("Server error fetching chart for {}: {} - {}",
                    symbol, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Chart fetch failed: Server error " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            log.error("Network error fetching chart for {}: {}", symbol, e.getMessage(), e);
            throw new RuntimeException("Chart fetch failed: Network error - " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error fetching chart for {}: {}", symbol, e.getMessage(), e);
            throw new RuntimeException("Chart fetch failed: " + e.getMessage(), e);
        }
    }
}
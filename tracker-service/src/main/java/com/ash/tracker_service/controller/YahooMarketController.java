package com.ash.tracker_service.controller;

import com.ash.tracker_service.service.TickerSearchService;
import com.ash.tracker_service.service.YahooMarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/yahoo")
@RequiredArgsConstructor
public class YahooMarketController {

    private final YahooMarketService yahooMarketService;
    private final TickerSearchService tickerSearchService;

    
    @GetMapping("/chart/{isin}")
    public Object getChartAndQuote(
            @PathVariable String isin,
            @RequestParam(defaultValue = "1d") String interval,
            @RequestParam(defaultValue = "1mo") String range) {

        String symbol = tickerSearchService.getSymbolByIsin(isin);
        return yahooMarketService.getChartAndQuote(symbol, interval, range);
    }

    @GetMapping("/index/{symbol}")
    public Object getIndexChart(

            @PathVariable String symbol,
            @RequestParam(defaultValue = "1d") String interval,
            @RequestParam(defaultValue = "1mo") String range) {

        System.out.println(symbol);
        return yahooMarketService.getIndexChartAndQuote(symbol, interval, range);
    }
}

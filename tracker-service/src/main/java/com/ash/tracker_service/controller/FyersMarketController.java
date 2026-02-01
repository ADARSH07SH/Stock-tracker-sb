package com.ash.tracker_service.controller;
import com.ash.tracker_service.service.FyersMarketService;
import com.ash.tracker_service.service.TickerSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/fyers")
@RequiredArgsConstructor
public class FyersMarketController {

    private final FyersMarketService fyersMarketService;
    private final TickerSearchService tickerSearchService;

    @GetMapping("/quote/{isin}")
    public Object getQuote(@PathVariable String isin) {
        String symbol = tickerSearchService.getSymbolByIsin(isin);
        return fyersMarketService.getQuote(symbol);
    }

    @GetMapping("/chart/{isin}")
    public Object getChart(
            @PathVariable String isin,
            @RequestParam String resolution,
            @RequestParam long from,
            @RequestParam long to) {

        String symbol = tickerSearchService.getSymbolByIsin(isin);
        return fyersMarketService.getChart(symbol, resolution, from, to);
    }
}

package com.ash.tracker_service.service;

import com.ash.tracker_service.entity.Ticker;
import com.ash.tracker_service.repository.TickerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TickerSearchService {

    private final TickerRepository tickerRepository;

    public List<Ticker> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        String regex = ".*" + Pattern.quote(query.trim()) + ".*";


        return tickerRepository
                .searchByQuery(regex)
                .stream()
                .limit(5)
                .toList();
    }

    public String getSymbolByIsin(String isin) {
        
        Ticker ticker = tickerRepository
                .findByIsinAndSource(isin, "NSE")
                .orElse(null);
        
        if (ticker != null) {
            return ticker.getSymbol();
        }
        
       
        System.out.println("NSE ticker not found for ISIN: " + isin + ", trying BSE");
        ticker = tickerRepository
                .findByIsinAndSource(isin, "BSE")
                .orElseThrow(() -> new RuntimeException("Ticker not found in NSE or BSE for ISIN: " + isin));
        
        return ticker.getSymbol();
    }
}

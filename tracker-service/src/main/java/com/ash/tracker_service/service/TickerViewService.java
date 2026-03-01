package com.ash.tracker_service.service;

import com.ash.tracker_service.entity.Ticker;
import com.ash.tracker_service.repository.TickerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TickerViewService {

    private final MongoTemplate mongoTemplate;
    private final TickerRepository tickerRepository;


    @Async
    public void incrementViewAsync(String symbol) {
        try {
            Query q = new Query(Criteria.where("symbol").regex("^" + symbol + "$", "i"));
            Update u = new Update()
                    .inc("viewCount", 1L)
                    .set("lastViewedAt", Instant.now());
            long modified = mongoTemplate.updateMulti(q, u, Ticker.class).getModifiedCount();
            if (modified == 0) {
                log.debug("incrementViewAsync: no ticker found for symbol '{}'", symbol);
            } else {
                log.debug("incrementViewAsync: +1 view for '{}' ({} row(s))", symbol, modified);
            }
        } catch (Exception e) {
            log.warn("incrementViewAsync failed for {}: {}", symbol, e.getMessage());
        }
    }


    public List<Ticker> getTopViewed() {
        return tickerRepository.findTop20ByOrderByViewCountDesc();
    }
}

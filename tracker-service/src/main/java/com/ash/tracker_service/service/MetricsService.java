package com.ash.tracker_service.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final Counter portfolioSyncCounter;
    private final Counter portfolioSyncFailureCounter;
    private final Timer marketDataFetchTimer;
    private final Counter apiRequestCounter;
    private final Counter userLoginCounter;
    private final Counter notificationSentCounter;
    private final MeterRegistry meterRegistry;

    public void incrementPortfolioSync() {
        portfolioSyncCounter.increment();
    }

    public void incrementPortfolioSyncFailure() {
        portfolioSyncFailureCounter.increment();
    }

    public Timer.Sample startMarketDataFetchTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordMarketDataFetch(Timer.Sample sample) {
        sample.stop(marketDataFetchTimer);
    }

    public void incrementApiRequest(String endpoint, String method) {
        apiRequestCounter.increment("endpoint", endpoint, "method", method);
    }

    public void incrementUserLogin() {
        userLoginCounter.increment();
    }

    public void incrementNotificationSent(String type) {
        notificationSentCounter.increment("type", type);
    }

    public void recordCustomGauge(String name, String description, double value) {
        meterRegistry.gauge(name, value);
    }

    public void recordBusinessMetric(String name, double value, String... tags) {
        meterRegistry.counter(name, tags).increment(value);
    }
}
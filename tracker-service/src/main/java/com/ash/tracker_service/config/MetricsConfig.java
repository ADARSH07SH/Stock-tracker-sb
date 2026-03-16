package com.ash.tracker_service.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public Counter portfolioSyncCounter(MeterRegistry meterRegistry) {
        return Counter.builder("portfolio_sync_total")
                .description("Total number of portfolio sync operations")
                .register(meterRegistry);
    }

    @Bean
    public Counter portfolioSyncFailureCounter(MeterRegistry meterRegistry) {
        return Counter.builder("portfolio_sync_failures_total")
                .description("Total number of failed portfolio sync operations")
                .register(meterRegistry);
    }

    @Bean
    public Timer marketDataFetchTimer(MeterRegistry meterRegistry) {
        return Timer.builder("market_data_fetch_duration_seconds")
                .description("Time taken to fetch market data")
                .register(meterRegistry);
    }

    @Bean
    public Counter apiRequestCounter(MeterRegistry meterRegistry) {
        return Counter.builder("api_requests_total")
                .description("Total number of API requests")
                .register(meterRegistry);
    }

    @Bean
    public Counter userLoginCounter(MeterRegistry meterRegistry) {
        return Counter.builder("user_logins_total")
                .description("Total number of user logins")
                .register(meterRegistry);
    }

    @Bean
    public Counter notificationSentCounter(MeterRegistry meterRegistry) {
        return Counter.builder("notifications_sent_total")
                .description("Total number of notifications sent")
                .register(meterRegistry);
    }
}
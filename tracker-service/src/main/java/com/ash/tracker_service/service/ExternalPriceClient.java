package com.ash.tracker_service.service;

import java.util.List;
import java.util.Map;

public interface ExternalPriceClient {
    Map<String, Double> fetchPrices(List<String> isins);
}

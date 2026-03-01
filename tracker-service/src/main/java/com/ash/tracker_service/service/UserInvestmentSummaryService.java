package com.ash.tracker_service.service;

import com.ash.tracker_service.dto.UserInvestmentSummaryDTO;

public interface UserInvestmentSummaryService {
    UserInvestmentSummaryDTO getSummary(String userId);
}

package com.ash.tracker_service.controller;

import com.ash.tracker_service.dto.UserInvestmentSummaryDTO;
import com.ash.tracker_service.service.UserInvestmentSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/summary")
@RequiredArgsConstructor
public class UserInvestmentSummaryController {

    private final UserInvestmentSummaryService summaryService;

    @GetMapping
    public UserInvestmentSummaryDTO getSummary(@RequestParam String userId) {
        System.out.println(userId);
        return summaryService.getSummary(userId);
    }
}

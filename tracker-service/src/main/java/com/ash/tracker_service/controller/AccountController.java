package com.ash.tracker_service.controller;

import com.ash.tracker_service.dto.AccountDTO;
import com.ash.tracker_service.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public void createAccount(@RequestBody AccountDTO dto) {
        accountService.create(dto);
    }

    @PatchMapping("/{accountId}")
    public void updateAccountName(
            @PathVariable String accountId,
            @RequestParam String userId,
            @RequestParam String accountName
    ) {
        accountService.updateName(userId, accountId, accountName);
    }

    @GetMapping
    public List<AccountDTO> getAccounts(@RequestParam String userId) {
        return accountService.getAccounts(userId);
    }
}

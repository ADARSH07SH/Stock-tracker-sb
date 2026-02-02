package com.ash.tracker_service.service;

import com.ash.tracker_service.dto.AccountDTO;
import com.ash.tracker_service.entity.Account;
import com.ash.tracker_service.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @Override
    public void create(AccountDTO dto) {

        Account account = Account.builder()
                .userId(dto.getUserId())
                .accountName(dto.getAccountName())
                .createdAt(Instant.now())
                .build();

        Account saved = accountRepository.save(account);
        System.out.println("Account created with ID: " + saved.getId() + " for user: " + saved.getUserId());
    }

    @Override
    public void updateName(String userId, String accountId, String accountName) {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized account access");
        }

        account.setAccountName(accountName);
        accountRepository.save(account);
        System.out.println("Account name updated: " + accountId + " -> " + accountName);
    }

    @Override
    public void deleteAccount(String accountId, String userId) {
        if(accountId==null||userId==null)
                throw new RuntimeException("Invalid parameters");
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        accountRepository.delete(account);
    }

    @Override
    public List<AccountDTO> getAccounts(String userId) {

        List<Account> accounts = accountRepository.findByUserId(userId);
        System.out.println("Found " + accounts.size() + " accounts for user: " + userId);
        
        return accounts.stream()
                .map(a -> {
                    AccountDTO dto = new AccountDTO();
                    dto.setUserId(a.getUserId());
                    dto.setAccountId(a.getId());
                    dto.setAccountName(a.getAccountName());
                    System.out.println("  - Account: " + a.getAccountName() + " (ID: " + a.getId() + ")");
                    return dto;
                })
                .toList();
    }
}

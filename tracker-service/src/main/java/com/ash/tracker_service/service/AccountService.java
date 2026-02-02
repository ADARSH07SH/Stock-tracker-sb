package com.ash.tracker_service.service;

import com.ash.tracker_service.dto.AccountDTO;

import java.util.List;

public interface AccountService {

    void create(AccountDTO dto);

    void updateName(String userId, String accountId, String accountName);

    void deleteAccount(String accountId, String userId);

    List<AccountDTO> getAccounts(String userId);
}

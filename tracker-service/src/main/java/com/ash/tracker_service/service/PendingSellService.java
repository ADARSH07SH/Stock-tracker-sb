package com.ash.tracker_service.service;

import com.ash.tracker_service.dto.PendingSellConfirmationDTO;

public interface PendingSellService {
    void confirm(PendingSellConfirmationDTO dto);
}

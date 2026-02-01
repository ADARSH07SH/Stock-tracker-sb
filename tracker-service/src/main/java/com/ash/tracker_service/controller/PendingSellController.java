package com.ash.tracker_service.controller;

import com.ash.tracker_service.dto.PendingSellConfirmationDTO;
import com.ash.tracker_service.service.PendingSellService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portfolio/pending-sell")
@RequiredArgsConstructor
public class PendingSellController {

    private final PendingSellService pendingSellService;

    @PostMapping("/confirm")
    public void confirmPendingSell(@RequestBody PendingSellConfirmationDTO dto) {
        pendingSellService.confirm(dto);
    }
}

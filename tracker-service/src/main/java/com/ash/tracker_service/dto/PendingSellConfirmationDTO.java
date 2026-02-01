package com.ash.tracker_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class PendingSellConfirmationDTO {

    private String userId;
    private String accountId;

    private List<PendingSellItemDTO> sells;
}

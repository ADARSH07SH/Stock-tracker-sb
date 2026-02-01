package com.ash.tracker_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExcelUploadRequestDTO {

    private String userId;
    private String accountId;

    private String mode; // APPEND or UPDATE
}

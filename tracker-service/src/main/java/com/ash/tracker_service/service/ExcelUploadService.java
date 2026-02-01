package com.ash.tracker_service.service;

import org.springframework.web.multipart.MultipartFile;

public interface ExcelUploadService {
    Object upload(String userId, String accountId, MultipartFile file, String mode);
}

package com.ash.tracker_service.controller;

import com.ash.tracker_service.service.ExcelUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/portfolio/upload")
@RequiredArgsConstructor
public class ExcelUploadController {

    private final ExcelUploadService excelUploadService;

    @PostMapping
    public Object uploadExcel(
            @RequestParam String userId,
            @RequestParam String accountId,
            @RequestParam String mode,
            @RequestPart MultipartFile file
    ) {
        return excelUploadService.upload(userId, accountId, file, mode);
    }
}

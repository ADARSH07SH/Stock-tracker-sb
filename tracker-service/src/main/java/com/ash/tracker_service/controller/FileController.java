package com.ash.tracker_service.controller;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final String UPLOAD_DIR = "uploads/";

    @GetMapping("/news/{filename}")
    public ResponseEntity<Resource> serveNewsImage(@PathVariable String filename) throws FileNotFoundException {
        Path filePath = Paths.get(UPLOAD_DIR + "news/" + filename);

        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }


        String contentType;
        try {
            contentType = Files.probeContentType(filePath);
        } catch (Exception e) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;

        Resource resource = new InputStreamResource(new FileInputStream(filePath.toFile()));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}

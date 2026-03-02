package com.ash.tracker_service.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "app_versions")
public class AppVersion {
    
    @Id
    private String id;
    
    private String version;
    private String downloadUrl;
    private String releaseNotes;
    private boolean forceUpdate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public AppVersion(String version, String downloadUrl, String releaseNotes, boolean forceUpdate) {
        this.version = version;
        this.downloadUrl = downloadUrl;
        this.releaseNotes = releaseNotes;
        this.forceUpdate = forceUpdate;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}

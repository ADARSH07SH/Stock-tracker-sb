package com.ash.tracker_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    private String userId;
    
    @Indexed(unique = true)
    private String email;
    
    private String password;
    
    private String name;
    
    private String profilePicture;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}

package org.elmo.robella.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    private Long id;
    
    private String username;
    
    private String email;
    
    private String fullName;
    
    private String avatar;
    
    private String phone;
    
    private Boolean active;
    
    private String role;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private LocalDateTime lastLoginAt;
    
    private Boolean emailVerified;
    
    private Boolean phoneVerified;
    
    private String githubId;
    
    private String provider;
    
    private String providerId;
}
package org.elmo.robella.model.response;

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
    
    private String displayName;
    
    private String avatar;
    
    private String phone;
    
    private Boolean active;
    
    private Integer role;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private LocalDateTime lastLoginAt;
    
    private String githubId;
}
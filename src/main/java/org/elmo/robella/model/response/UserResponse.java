package org.elmo.robella.model.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.OffsetDateTime;

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

    private Boolean active;
    
    private String role;
    
    private OffsetDateTime createdAt;
    
    private OffsetDateTime updatedAt;
    
    private OffsetDateTime lastLoginAt;
    
    private String githubId;
}
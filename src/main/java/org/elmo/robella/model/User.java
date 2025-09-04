package org.elmo.robella.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User {
    
    @Id
    private Long id;
    
    private String username;
    
    private String email;
    
    private String password;
    
    private String fullName;
    
    private String avatar;
    
    private String phone;
    
    private Boolean active;
    
    private String role;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    private LocalDateTime lastLoginAt;
    
    private String emailVerified;
    
    private String phoneVerified;
    
    private String githubId;
    
    private String provider;
    
    private String providerId;
}
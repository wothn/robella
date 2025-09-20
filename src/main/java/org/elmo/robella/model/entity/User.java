package org.elmo.robella.model.entity;

import org.elmo.robella.model.common.Role;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.OffsetDateTime;

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
    
    private String displayName;
    
    private String avatar;

    private Boolean active;
    private Role role;
    
    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("updated_at")
    private OffsetDateTime updatedAt;

    @Column("last_login_at")
    private OffsetDateTime lastLoginAt;
    
    @Column("github_id")
    private String githubId;
}
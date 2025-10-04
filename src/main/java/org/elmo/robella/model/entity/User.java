package org.elmo.robella.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import org.elmo.robella.handler.SQLiteOffsetDateTimeTypeHandler;
import org.elmo.robella.model.common.Role;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "users", autoResultMap = true)
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String email;

    private String password;

    private String displayName;

    private String avatar;

    private BigDecimal credits;

    private Boolean active;

    private Role role;

    @TableField(value = "created_at", typeHandler = SQLiteOffsetDateTimeTypeHandler.class, fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(value = "updated_at", typeHandler = SQLiteOffsetDateTimeTypeHandler.class, fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(value = "last_login_at", typeHandler = SQLiteOffsetDateTimeTypeHandler.class)
    private OffsetDateTime lastLoginAt;

    @TableField("github_id")
    private String githubId;
}
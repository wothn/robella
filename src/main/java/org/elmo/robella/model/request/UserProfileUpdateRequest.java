package org.elmo.robella.model.request;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {

    @Size(max = 50, message = "用户名长度不能超过50个字符")
    private String username;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Size(max = 100, message = "显示名称长度不能超过100个字符")
    private String displayName;

  
    @Size(max = 500, message = "头像URL长度不能超过500个字符")
    private String avatar;
}
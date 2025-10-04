package org.elmo.robella.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    @Email(message = "邮箱格式不正确")
    private String email;

    @Size(max = 20, message = "显示名称不能超过20个字符")
    private String displayName;

    private String avatar;

    private Boolean active;

    private String role;

    private BigDecimal credits;
}
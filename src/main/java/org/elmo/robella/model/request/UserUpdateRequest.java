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

    @Size(max = 100, message = "显示名称不能超过100个字符")
    private String displayName;

    private String avatar;

    private Boolean active;

    private String role;

    // Added for tracking user credits
    private BigDecimal credits;

    public BigDecimal getCredits() {
        return credits;
    }

    public void setCredits(BigDecimal credits) {
        this.credits = credits;
    }
}
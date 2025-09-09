package org.elmo.robella.model.request;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordChangeRequest {
    
    private String currentPassword;
    
    private String newPassword;
    
    private String confirmPassword;
}
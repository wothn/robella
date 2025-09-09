package org.elmo.robella.model.request;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequest {
    
    private String email;
    
    private String token;
    
    private String password;
    
    private String confirmPassword;
}
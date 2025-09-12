package org.elmo.robella.model.request;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    
    private String username;
    
    private String email;
    
    private String password;
    
    private String confirmPassword;
    
    private String displayName;
    
    private String phone;
}
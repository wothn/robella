package org.elmo.robella.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.elmo.robella.model.common.OpenAIMessage;

import java.util.List;

@Data
public class OpenAIChatRequest {
    @NotBlank
    private String model;
    
    @NotEmpty
    private List<OpenAIMessage> messages;
    
    private Double temperature;
    private Integer maxTokens;
    private Boolean stream = false;
    private String user;
}
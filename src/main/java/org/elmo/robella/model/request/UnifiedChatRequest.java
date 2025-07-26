package org.elmo.robella.model.request;

import lombok.Data;
import org.elmo.robella.model.common.UnifiedMessage;

import java.util.List;

@Data
public class UnifiedChatRequest {
    private String model;
    private List<UnifiedMessage> messages;
    private Double temperature;
    private Integer maxTokens;
    private Boolean stream;
    private String userId;
}
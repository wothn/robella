package org.elmo.robella.model.response;

import lombok.Data;
import org.elmo.robella.model.common.UnifiedMessage;
import org.elmo.robella.model.common.Usage;

@Data
public class UnifiedChatResponse {
    private String id;
    private String model;
    private UnifiedMessage message;
    private Usage usage;
}
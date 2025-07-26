package org.elmo.robella.model.common;

import lombok.Data;

@Data
public class UnifiedMessage {
    private String role;
    private String content;
}
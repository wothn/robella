package org.elmo.robella.model.response;

import lombok.Data;

@Data
public class GeminiContent {
    private String role;
    private String parts;
}
package org.elmo.robella.model.request;

import lombok.Data;

@Data
public class GeminiContent {
    private String role;
    private String parts;
}
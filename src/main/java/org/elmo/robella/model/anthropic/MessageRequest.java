package org.elmo.robella.model.anthropic;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageRequest {

    /**
     * 使用的模型名称
     */
    private String model;


}

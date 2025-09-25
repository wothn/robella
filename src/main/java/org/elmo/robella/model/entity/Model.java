package org.elmo.robella.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.handler.ModelCapabilityTypeHandler;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Slf4j
@TableName("model")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Model {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    @TableField("model_key")
    private String modelKey;

    private String description;
    private String organization;

    @TableField(typeHandler = ModelCapabilityTypeHandler.class)
    private List<ModelCapability> capabilities;

    @TableField("context_window")
    private Integer contextWindow;

    private Boolean published;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.UPDATE)
    private OffsetDateTime updatedAt;

}
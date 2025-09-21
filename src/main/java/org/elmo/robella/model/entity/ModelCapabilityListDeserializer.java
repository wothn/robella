package org.elmo.robella.model.entity;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModelCapabilityListDeserializer extends JsonDeserializer<List<ModelCapability>> {

    @Override
    public List<ModelCapability> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String[] enumNames = p.readValueAs(String[].class);

        if (enumNames == null || enumNames.length == 0) {
            return new ArrayList<>();
        }

        List<ModelCapability> capabilities = new ArrayList<>();
        for (String enumName : enumNames) {
            try {
                capabilities.add(ModelCapability.valueOf(enumName));
            } catch (IllegalArgumentException e) {
                // 忽略无效的枚举值，也可以选择抛出异常或记录日志
                // 这里选择静默忽略以保持向后兼容性
            }
        }

        return capabilities;
    }
}
package org.elmo.robella.model.entity;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.List;

public class ModelCapabilityListSerializer extends JsonSerializer<List<ModelCapability>> {

    @Override
    public void serialize(List<ModelCapability> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null || value.isEmpty()) {
            gen.writeNull();
            return;
        }

        // 将枚举值转换为字符串数组，然后序列化为JSON字符串
        String[] enumNames = value.stream()
                .map(ModelCapability::name)
                .toArray(String[]::new);

        gen.writeObject(enumNames);
    }
}
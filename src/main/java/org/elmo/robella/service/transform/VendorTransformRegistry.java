package org.elmo.robella.service.transform;

import org.elmo.robella.util.ConfigUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 预注册所有 VendorTransform，实现按 provider type 查找。
 */
@Component
public class VendorTransformRegistry {
    private final Map<String, VendorTransform> registry;

    public VendorTransformRegistry(ConfigUtils configUtils) {
        // 手动注册；若后续改为通过 Spring 注入 List<VendorTransform>，可切换构造签名
        List<VendorTransform> transforms = List.of(
                new OpenAITransform(configUtils),
                new AnthropicTransform(configUtils)
        );
        this.registry = transforms.stream().collect(Collectors.toMap(VendorTransform::type, Function.identity()));
    }

    public VendorTransform get(String type) {
        return registry.getOrDefault(type == null ? "OpenAI" : type, registry.get("OpenAI"));
    }
}

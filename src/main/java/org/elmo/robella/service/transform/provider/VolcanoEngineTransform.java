package org.elmo.robella.service.transform.provider;

import org.elmo.robella.common.ProviderType;
import org.elmo.robella.model.openai.core.ChatCompletionRequest;
import org.elmo.robella.model.openai.core.ChatCompletionResponse;
import org.elmo.robella.model.openai.core.Thinking;
import org.elmo.robella.util.OpenAITransformUtils;
import org.springframework.stereotype.Component;

@Component
public class VolcanoEngineTransform implements VendorTransform<ChatCompletionRequest, ChatCompletionResponse>{

    public ProviderType providerType() {
        return ProviderType.VOLCANOENGINE;
    }

    public ChatCompletionRequest processRequest(ChatCompletionRequest req) {
        if (req.getReasoningEffort() != null) {
            Thinking thinking = new Thinking();
            String effort = req.getReasoningEffort();
            String type = OpenAITransformUtils.mapReasoningToThinkingType(effort);
            thinking.setType(type);
            req.setThinking(thinking);
        }
        req.setReasoningEffort(null);
        return req;
    }


}

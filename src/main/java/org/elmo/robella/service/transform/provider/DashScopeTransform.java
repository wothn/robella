package org.elmo.robella.service.transform.provider;

import org.elmo.robella.common.ProviderType;
import org.elmo.robella.model.openai.core.ChatCompletionRequest;
import org.elmo.robella.model.openai.core.ChatCompletionResponse;
import org.springframework.stereotype.Component;

@Component
public class DashScopeTransform implements VendorTransform<ChatCompletionRequest, ChatCompletionResponse>{
    @Override
    public ProviderType providerType() {
        return ProviderType.DASHSCOPE;
    }

    @Override
    public ChatCompletionRequest processRequest(ChatCompletionRequest req) {
        if (req.getReasoningEffort() != null) {
            req.setEnableThinking(true);
        }
        req.setReasoningEffort(null);
        return req;
    }

}

package org.elmo.robella.service.transform.provider;

import org.elmo.robella.common.ProviderType;
import org.elmo.robella.model.openai.core.ChatCompletionRequest;
import org.elmo.robella.model.openai.core.ChatCompletionResponse;

public class OpenAITransform implements VendorTransform<ChatCompletionRequest, ChatCompletionResponse> {

    @Override
    public ProviderType providerType() {
        return ProviderType.OPENAI;
    }

    @Override
    public ChatCompletionRequest processRequest(ChatCompletionRequest req) {
        if (req.getMaxTokens() != null) {
            req.setMaxCompletionsTokens(req.getMaxTokens());
        }
        return req;
    }

}

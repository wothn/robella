import org.elmo.robella.model.openai.ChatCompletionChunk;
import org.elmo.robella.util.JsonUtils;

public class TestToolCallParsing {
    public static void main(String[] args) {
        String json = """
        {
            "id": "chatcmpl-9a00a3cccb2148e8a88874b0a1d2ec92",
            "object": "chat.completion.chunk",
            "created": 1755094586,
            "model": "gemini",
            "choices": [{
                "index": 0,
                "delta": {
                    "tool_calls": [{
                        "id": "call_a473c5aec23a4f6fa7d6526e73c0c30a",
                        "type": "function",
                        "function": {
                            "name": "get_current_time",
                            "arguments": "{\\"timezone\\":\\"Asia/Shanghai\\"}"
                        }
                    }]
                },
                "finish_reason": "tool_calls"
            }],
            "usage": {
                "prompt_tokens": 201,
                "completion_tokens": 9,
                "total_tokens": 210
            }
        }
        """;
        
        try {
            ChatCompletionChunk chunk = JsonUtils.fromJson(json, ChatCompletionChunk.class);
            System.out.println("Parsed successfully: " + chunk);
            
            if (chunk != null && chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
                var choice = chunk.getChoices().get(0);
                var delta = choice.getDelta();
                if (delta != null && delta.getToolCalls() != null) {
                    for (var toolCall : delta.getToolCalls()) {
                        System.out.println("ToolCall: " + toolCall);
                        System.out.println("Function: " + toolCall.getFunction());
                        if (toolCall.getFunction() != null) {
                            System.out.println("Function.function: " + toolCall.getFunction().getFunction());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

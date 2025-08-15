package org.elmo.robella;

import org.elmo.robella.model.openai.ChatCompletionChunk;
import org.elmo.robella.service.transform.OpenAIVendorTransform;
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
            System.out.println("=== Testing ToolCall JSON Parsing ===");
            
            // 1. 测试基本的JSON解析
            ChatCompletionChunk chunk = JsonUtils.fromJson(json, ChatCompletionChunk.class);
            System.out.println("✓ JSON parsed successfully");
            
            if (chunk != null && chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
                var choice = chunk.getChoices().get(0);
                var delta = choice.getDelta();
                
                if (delta != null && delta.getToolCalls() != null && !delta.getToolCalls().isEmpty()) {
                    var toolCall = delta.getToolCalls().get(0);
                    System.out.println("✓ ToolCall found:");
                    System.out.println("  ID: " + toolCall.getId());
                    System.out.println("  Type: " + toolCall.getType());
                    
                    if (toolCall.getFunction() != null) {
                        System.out.println("  Function Name: " + toolCall.getFunction().getName());
                        System.out.println("  Function Arguments: " + toolCall.getFunction().getArguments());
                    }
                }
            }
            
            // 2. 测试 OpenAIVendorTransform 转换
            System.out.println("\\n=== Testing OpenAIVendorTransform ===");
            OpenAIVendorTransform transform = new OpenAIVendorTransform();
            var unifiedChunk = transform.vendorStreamEventToUnified(json);
            
            if (unifiedChunk != null) {
                System.out.println("✓ Transform successful");
                System.out.println("  Content Delta: " + unifiedChunk.getContentDelta());
                System.out.println("  Tool Call Deltas: " + unifiedChunk.getToolCallDeltas());
                System.out.println("  Finished: " + unifiedChunk.isFinished());
                System.out.println("  Finish Reason: " + unifiedChunk.getFinishReason());
                
                if (unifiedChunk.getToolCallDeltas() != null && !unifiedChunk.getToolCallDeltas().isEmpty()) {
                    var toolCallDelta = unifiedChunk.getToolCallDeltas().get(0);
                    System.out.println("  First Tool Call:");
                    System.out.println("    ID: " + toolCallDelta.getId());
                    System.out.println("    Name: " + toolCallDelta.getName());
                    System.out.println("    Arguments: " + toolCallDelta.getArgumentsDelta());
                }
            } else {
                System.out.println("✗ Transform failed - returned null");
            }
            
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

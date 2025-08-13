import org.elmo.robella.service.transform.OpenAIVendorTransform;
import org.elmo.robella.model.openai.*;
import org.elmo.robella.model.internal.*;
import java.util.*;

/**
 * 简单测试 OpenAIVendorTransform 的基本功能
 */
public class TestTransform {
    public static void main(String[] args) {
        OpenAIVendorTransform transform = new OpenAIVendorTransform();
        
        // 测试基本请求转换
        System.out.println("Testing OpenAI vendor transform...");
        
        // 创建一个简单的OpenAI请求
        ChatCompletionRequest openaiRequest = new ChatCompletionRequest();
        openaiRequest.setModel("gpt-4");
        openaiRequest.setMaxTokens(100);
        openaiRequest.setTemperature(0.7);
        
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage message = ChatMessage.builder()
                .role("user")
                .content(List.of(ContentPart.ofText("Hello")))
                .build();
        messages.add(message);
        openaiRequest.setMessages(messages);
        
        // 转换为统一格式
        UnifiedChatRequest unified = transform.vendorRequestToUnified(openaiRequest);
        System.out.println("Unified request model: " + unified.getModel());
        System.out.println("Unified request messages count: " + unified.getMessages().size());
        System.out.println("First message role: " + unified.getMessages().get(0).getRole());
        System.out.println("First message content count: " + unified.getMessages().get(0).getContents().size());
        
        // 转换回OpenAI格式
        Object convertedBack = transform.unifiedToVendorRequest(unified);
        if (convertedBack instanceof ChatCompletionRequest backRequest) {
            System.out.println("Converted back model: " + backRequest.getModel());
            System.out.println("Converted back messages count: " + backRequest.getMessages().size());
            System.out.println("Back message role: " + backRequest.getMessages().get(0).getRole());
        }
        
        System.out.println("Transform test completed successfully!");
    }
}

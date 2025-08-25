package org.elmo.robella.service.transform;

import org.elmo.robella.model.anthropic.core.*;
import org.elmo.robella.model.anthropic.tool.*;
import org.elmo.robella.model.anthropic.content.*;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.ThinkingOptions;
import org.elmo.robella.model.openai.core.OpenAIMessage;
import org.elmo.robella.model.openai.tool.Tool;
import org.elmo.robella.model.openai.tool.ToolChoice;
import org.elmo.robella.model.openai.content.OpenAITextContent;
import org.elmo.robella.model.openai.content.OpenAIContent;
import org.elmo.robella.util.ConfigUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * AnthropicTransform 测试类
 * 测试 Anthropic 请求与统一请求格式之间的双向转换
 */
@ExtendWith(MockitoExtension.class)
class AnthropicTransformTest {

    @Mock
    private ConfigUtils configUtils;

    private AnthropicTransform anthropicTransform;

    @BeforeEach
    void setUp() {
        anthropicTransform = new AnthropicTransform(configUtils);
    }

    @Test
    void testVendorRequestToUnified_BasicRequest() {
        // Mock ConfigUtils 行为 
        when(configUtils.getThinkingField(anyString(), anyString())).thenReturn("thinking");
        
        // 准备测试数据
        AnthropicChatRequest anthropicRequest = createBasicAnthropicRequest();
        
        // 执行转换
        UnifiedChatRequest result = anthropicTransform.vendorRequestToUnified(anthropicRequest);
        
        // 验证基础字段
        assertNotNull(result);
        assertEquals("claude-3-5-sonnet-20241022", result.getModel());
        assertEquals(true, result.getStream());
        assertEquals(1000, result.getMaxTokens());
        assertEquals(0.7, result.getTemperature());
        assertEquals(0.9, result.getTopP());
        assertEquals(10, result.getTopK());
        assertNotNull(result.getStop());
        assertEquals(Arrays.asList("stop1", "stop2"), result.getStop());
        
        // 验证 StreamOptions
        assertNotNull(result.getStreamOptions());
        assertTrue(result.getStreamOptions().getIncludeUsage());
        
        // 验证临时字段
        assertNotNull(result.getTempFields());
        assertEquals("thinking", result.getTempFields().get("config_thinking"));
        
        // 验证消息转换
        assertNotNull(result.getMessages());
        assertEquals(2, result.getMessages().size());
        
        // 验证系统消息转换
        OpenAIMessage systemMessage = result.getMessages().get(0);
        assertEquals("system", systemMessage.getRole());
        assertEquals("You are a helpful assistant.", ((OpenAITextContent) systemMessage.getContent().get(0)).getText());
        
        // 验证用户消息
        OpenAIMessage userMessage = result.getMessages().get(1);
        assertEquals("user", userMessage.getRole());
        assertEquals("Hello, how are you?", ((OpenAITextContent) userMessage.getContent().get(0)).getText());
    }

    @Test
    void testVendorRequestToUnified_WithTools() {
        // Mock ConfigUtils 行为 
        when(configUtils.getThinkingField(anyString(), anyString())).thenReturn("thinking");
        
        // 准备带工具的测试数据
        AnthropicChatRequest anthropicRequest = createAnthropicRequestWithTools();
        
        // 执行转换
        UnifiedChatRequest result = anthropicTransform.vendorRequestToUnified(anthropicRequest);
        
        // 验证工具转换
        assertNotNull(result.getTools());
        assertEquals(1, result.getTools().size());
        
        Tool tool = result.getTools().get(0);
        assertEquals("function", tool.getType());
        assertEquals("get_weather", tool.getFunction().getName());
        assertEquals("Get weather information for a location", tool.getFunction().getDescription());
        assertNotNull(tool.getFunction().getParameters());
        
        // 验证工具选择转换
        assertNotNull(result.getToolChoice());
        assertEquals("required", result.getToolChoice().getType());
    }

    @Test
    void testVendorRequestToUnified_WithThinking() {
        // Mock ConfigUtils 行为 
        when(configUtils.getThinkingField(anyString(), anyString())).thenReturn("thinking");
        
        // 准备带思考功能的测试数据
        AnthropicChatRequest anthropicRequest = createAnthropicRequestWithThinking();
        
        // 执行转换
        UnifiedChatRequest result = anthropicTransform.vendorRequestToUnified(anthropicRequest);
        
        // 验证思考选项转换
        assertNotNull(result.getThinkingOptions());
        assertEquals("enabled", result.getThinkingOptions().getType());
    }

    @Test
    void testVendorRequestToUnified_InvalidInput() {
        // 测试无效输入
        UnifiedChatRequest result = anthropicTransform.vendorRequestToUnified("invalid");
        assertNull(result);
        
        result = anthropicTransform.vendorRequestToUnified(null);
        assertNull(result);
    }

    @Test
    void testUnifiedToVendorRequest_BasicRequest() {
        // 准备测试数据
        UnifiedChatRequest unifiedRequest = createBasicUnifiedRequest();
        
        // 执行转换
        Object result = anthropicTransform.unifiedToVendorRequest(unifiedRequest);
        
        // 验证返回类型
        assertNotNull(result);
        assertTrue(result instanceof AnthropicChatRequest);
        
        AnthropicChatRequest anthropicRequest = (AnthropicChatRequest) result;
        
        // 验证基础字段转换
        assertEquals("claude-3-5-sonnet-20241022", anthropicRequest.getModel());
        assertEquals(true, anthropicRequest.getStream());
        assertEquals(1000, anthropicRequest.getMaxTokens());
        assertEquals(0.7, anthropicRequest.getTemperature());
        assertEquals(0.9, anthropicRequest.getTopP());
        assertEquals(10, anthropicRequest.getTopK());
        assertNotNull(anthropicRequest.getStopSequences());
        assertEquals(Arrays.asList("stop1", "stop2"), anthropicRequest.getStopSequences());
        
        // 验证系统消息转换
        assertEquals("You are a helpful assistant.", anthropicRequest.getSystem());
        
        // 验证消息转换
        assertNotNull(anthropicRequest.getMessages());
        assertEquals(1, anthropicRequest.getMessages().size());
        
        AnthropicMessage userMessage = anthropicRequest.getMessages().get(0);
        assertEquals("user", userMessage.getRole());
        assertNotNull(userMessage.getContent());
        assertEquals(1, userMessage.getContent().size());
        assertTrue(userMessage.getContent().get(0) instanceof AnthropicTextContent);
        assertEquals("Hello, how are you?", ((AnthropicTextContent) userMessage.getContent().get(0)).getText());
    }

    @Test
    void testUnifiedToVendorRequest_WithTools() {
        // 准备带工具的测试数据
        UnifiedChatRequest unifiedRequest = createUnifiedRequestWithTools();
        
        // 执行转换
        Object result = anthropicTransform.unifiedToVendorRequest(unifiedRequest);
        
        assertTrue(result instanceof AnthropicChatRequest);
        AnthropicChatRequest anthropicRequest = (AnthropicChatRequest) result;
        
        // 验证工具转换
        assertNotNull(anthropicRequest.getTools());
        assertEquals(1, anthropicRequest.getTools().size());
        
        AnthropicTool tool = anthropicRequest.getTools().get(0);
        assertTrue(tool instanceof AnthropicCustomTool);
        AnthropicCustomTool customTool = (AnthropicCustomTool) tool;
        assertEquals("get_weather", customTool.getName());
        assertEquals("Get weather information for a location", customTool.getDescription());
        assertNotNull(customTool.getInputSchema());
        
        // 验证工具选择转换
        assertNotNull(anthropicRequest.getToolChoice());
        assertEquals("auto", anthropicRequest.getToolChoice().getType());
    }

    @Test
    void testUnifiedToVendorRequest_WithThinking() {
        // 准备带思考功能的测试数据
        UnifiedChatRequest unifiedRequest = createUnifiedRequestWithThinking();
        
        // 执行转换
        Object result = anthropicTransform.unifiedToVendorRequest(unifiedRequest);
        
        assertTrue(result instanceof AnthropicChatRequest);
        AnthropicChatRequest anthropicRequest = (AnthropicChatRequest) result;
        
        // 验证思考功能转换
        assertNotNull(anthropicRequest.getThinking());
        assertTrue(anthropicRequest.getThinking().isEnabled());
    }

    @Test
    void testBidirectionalConversion() {
        // Mock ConfigUtils 行为 
        when(configUtils.getThinkingField(anyString(), anyString())).thenReturn("thinking");
        
        // 测试双向转换的一致性
        AnthropicChatRequest originalRequest = createBasicAnthropicRequest();
        
        // Anthropic -> Unified
        UnifiedChatRequest unifiedRequest = anthropicTransform.vendorRequestToUnified(originalRequest);
        assertNotNull(unifiedRequest);
        
        // Unified -> Anthropic
        Object reconvertedObject = anthropicTransform.unifiedToVendorRequest(unifiedRequest);
        assertTrue(reconvertedObject instanceof AnthropicChatRequest);
        
        AnthropicChatRequest reconvertedRequest = (AnthropicChatRequest) reconvertedObject;
        
        // 验证关键字段保持一致
        assertEquals(originalRequest.getModel(), reconvertedRequest.getModel());
        assertEquals(originalRequest.getStream(), reconvertedRequest.getStream());
        assertEquals(originalRequest.getMaxTokens(), reconvertedRequest.getMaxTokens());
        assertEquals(originalRequest.getTemperature(), reconvertedRequest.getTemperature());
        assertEquals(originalRequest.getTopP(), reconvertedRequest.getTopP());
        assertEquals(originalRequest.getTopK(), reconvertedRequest.getTopK());
        assertEquals(originalRequest.getSystem(), reconvertedRequest.getSystem());
        assertEquals(originalRequest.getStopSequences(), reconvertedRequest.getStopSequences());
    }

    @Test
    void testTypeMethod() {
        assertEquals("Anthropic", anthropicTransform.type());
    }

    // 辅助方法：创建基础 Anthropic 请求
    private AnthropicChatRequest createBasicAnthropicRequest() {
        AnthropicChatRequest request = new AnthropicChatRequest();
        request.setModel("claude-3-5-sonnet-20241022");
        request.setStream(true);
        request.setMaxTokens(1000);
        request.setTemperature(0.7);
        request.setTopP(0.9);
        request.setTopK(10);
        request.setStopSequences(Arrays.asList("stop1", "stop2"));
        request.setSystem("You are a helpful assistant.");
        
        // 添加消息
        List<AnthropicMessage> messages = new ArrayList<>();
        AnthropicMessage userMessage = new AnthropicMessage();
        userMessage.setRole("user");
        
        List<AnthropicContent> content = new ArrayList<>();
        AnthropicTextContent textContent = new AnthropicTextContent();
        textContent.setText("Hello, how are you?");
        content.add(textContent);
        userMessage.setContent(content);
        
        messages.add(userMessage);
        request.setMessages(messages);
        
        return request;
    }

    // 辅助方法：创建带工具的 Anthropic 请求
    private AnthropicChatRequest createAnthropicRequestWithTools() {
        AnthropicChatRequest request = createBasicAnthropicRequest();
        
        // 添加工具
        List<AnthropicTool> tools = new ArrayList<>();
        AnthropicCustomTool tool = new AnthropicCustomTool();
        tool.setName("get_weather");
        tool.setDescription("Get weather information for a location");
        
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> locationProperty = new HashMap<>();
        locationProperty.put("type", "string");
        locationProperty.put("description", "The location to get weather for");
        properties.put("location", locationProperty);
        schema.put("properties", properties);
        schema.put("required", Arrays.asList("location"));
        
        tool.setInputSchema(schema);
        tools.add(tool);
        request.setTools(tools);
        
        // 设置工具选择
        AnthropicToolChoice toolChoice = new AnthropicToolChoice();
        toolChoice.setType("any");
        request.setToolChoice(toolChoice);
        
        return request;
    }

    // 辅助方法：创建带思考功能的 Anthropic 请求
    private AnthropicChatRequest createAnthropicRequestWithThinking() {
        AnthropicChatRequest request = createBasicAnthropicRequest();
        
        AnthropicThinking thinking = AnthropicThinking.enabled();
        request.setThinking(thinking);
        
        return request;
    }

    // 辅助方法：创建基础统一请求
    private UnifiedChatRequest createBasicUnifiedRequest() {
        UnifiedChatRequest request = new UnifiedChatRequest();
        request.setModel("claude-3-5-sonnet-20241022");
        request.setStream(true);
        request.setMaxTokens(1000);
        request.setTemperature(0.7);
        request.setTopP(0.9);
        request.setTopK(10);
        request.setStop(Arrays.asList("stop1", "stop2"));
        
        // 添加消息
        List<OpenAIMessage> messages = new ArrayList<>();
        
        // 系统消息
        OpenAIMessage systemMessage = new OpenAIMessage();
        systemMessage.setRole("system");
        List<OpenAIContent> systemContent = new ArrayList<>();
        OpenAITextContent systemTextContent = new OpenAITextContent();
        systemTextContent.setText("You are a helpful assistant.");
        systemContent.add(systemTextContent);
        systemMessage.setContent(systemContent);
        messages.add(systemMessage);
        
        // 用户消息
        OpenAIMessage userMessage = new OpenAIMessage();
        userMessage.setRole("user");
        List<OpenAIContent> userContent = new ArrayList<>();
        OpenAITextContent userTextContent = new OpenAITextContent();
        userTextContent.setText("Hello, how are you?");
        userContent.add(userTextContent);
        userMessage.setContent(userContent);
        messages.add(userMessage);
        
        request.setMessages(messages);
        
        return request;
    }

    // 辅助方法：创建带工具的统一请求
    private UnifiedChatRequest createUnifiedRequestWithTools() {
        UnifiedChatRequest request = createBasicUnifiedRequest();
        
        // 添加工具
        List<Tool> tools = new ArrayList<>();
        Tool tool = new Tool();
        tool.setType("function");
        
        org.elmo.robella.model.openai.tool.Function function = new org.elmo.robella.model.openai.tool.Function();
        function.setName("get_weather");
        function.setDescription("Get weather information for a location");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> locationProperty = new HashMap<>();
        locationProperty.put("type", "string");
        locationProperty.put("description", "The location to get weather for");
        properties.put("location", locationProperty);
        parameters.put("properties", properties);
        parameters.put("required", Arrays.asList("location"));
        
        function.setParameters(parameters);
        tool.setFunction(function);
        tools.add(tool);
        request.setTools(tools);
        
        // 设置工具选择
        ToolChoice toolChoice = new ToolChoice();
        toolChoice.setType("auto");
        request.setToolChoice(toolChoice);
        
        return request;
    }

    // 辅助方法：创建带思考功能的统一请求
    private UnifiedChatRequest createUnifiedRequestWithThinking() {
        UnifiedChatRequest request = createBasicUnifiedRequest();
        
        ThinkingOptions thinkingOptions = new ThinkingOptions();
        thinkingOptions.setType("enabled");
        request.setThinkingOptions(thinkingOptions);
        
        return request;
    }
}

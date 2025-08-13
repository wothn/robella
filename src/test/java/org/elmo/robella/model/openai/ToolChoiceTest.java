package org.elmo.robella.model.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ToolChoice测试类
 */
public class ToolChoiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testStringTypeSerialization() throws Exception {
        // 测试字符串类型序列化
        ToolChoice auto = ToolChoice.AUTO;
        String json = objectMapper.writeValueAsString(auto);
        assertEquals("\"auto\"", json);
        
        ToolChoice none = ToolChoice.NONE;
        json = objectMapper.writeValueAsString(none);
        assertEquals("\"none\"", json);
        
        ToolChoice required = ToolChoice.REQUIRED;
        json = objectMapper.writeValueAsString(required);
        assertEquals("\"required\"", json);
    }

    @Test
    public void testStringTypeDeserialization() throws Exception {
        // 测试字符串类型反序列化
        ToolChoice auto = objectMapper.readValue("\"auto\"", ToolChoice.class);
        assertEquals("auto", auto.getType());
        assertTrue(auto.isStringType());
        
        ToolChoice none = objectMapper.readValue("\"none\"", ToolChoice.class);
        assertEquals("none", none.getType());
        assertTrue(none.isStringType());
    }

    @Test
    public void testFunctionTypeSerialization() throws Exception {
        // 测试函数类型序列化
        ToolChoice function = ToolChoice.function("calculate");
        String json = objectMapper.writeValueAsString(function);
        assertTrue(json.contains("\"type\":\"function\""));
        assertTrue(json.contains("\"function\""));
        assertTrue(json.contains("\"name\":\"calculate\""));
    }

    @Test
    public void testFunctionTypeDeserialization() throws Exception {
        // 测试函数类型反序列化
        String json = "{\"type\":\"function\",\"function\":{\"name\":\"calculate\"}}";
        ToolChoice function = objectMapper.readValue(json, ToolChoice.class);
        assertEquals("function", function.getType());
        assertNotNull(function.getFunction());
        assertEquals("calculate", function.getFunction().getName());
        assertFalse(function.isStringType());
    }

    @Test
    public void testAllowedToolsType() throws Exception {
        // 测试allowed_tools类型
        Tool tool1 = new Tool();
        tool1.setType("function");
        Function func1 = new Function();
        func1.setName("tool1");
        tool1.setFunction(func1);
        
        Tool tool2 = new Tool();
        tool2.setType("function");
        Function func2 = new Function();
        func2.setName("tool2");
        tool2.setFunction(func2);
        
        List<Tool> tools = Arrays.asList(tool1, tool2);
        ToolChoice allowedTools = ToolChoice.allowedTools("auto", tools);
        
        assertEquals("allowed_tools", allowedTools.getType());
        assertNotNull(allowedTools.getAllowedTools());
        assertEquals("auto", allowedTools.getAllowedTools().getMode());
        assertEquals(2, allowedTools.getAllowedTools().getTools().size());
        assertFalse(allowedTools.isStringType());
        
        // 测试序列化
        String json = objectMapper.writeValueAsString(allowedTools);
        assertTrue(json.contains("\"type\":\"allowed_tools\""));
        assertTrue(json.contains("\"allowed_tools\""));
        assertTrue(json.contains("\"mode\":\"auto\""));
    }

    @Test
    public void testCustomTypeSerialization() throws Exception {
        // 测试custom类型序列化
        ToolChoice custom = ToolChoice.custom("my_custom_tool");
        String json = objectMapper.writeValueAsString(custom);
        assertTrue(json.contains("\"type\":\"custom\""));
        assertTrue(json.contains("\"custom\""));
        assertTrue(json.contains("\"name\":\"my_custom_tool\""));
    }

    @Test
    public void testCustomTypeDeserialization() throws Exception {
        // 测试custom类型反序列化
        String json = "{\"type\":\"custom\",\"custom\":{\"name\":\"my_custom_tool\"}}";
        ToolChoice custom = objectMapper.readValue(json, ToolChoice.class);
        assertEquals("custom", custom.getType());
        assertNotNull(custom.getCustom());
        assertEquals("my_custom_tool", custom.getCustom().getName());
        assertFalse(custom.isStringType());
    }

    @Test
    public void testPreDefinedConstants() {
        // 测试预定义常量
        assertEquals("none", ToolChoice.NONE.getType());
        assertEquals("auto", ToolChoice.AUTO.getType());
        assertEquals("required", ToolChoice.REQUIRED.getType());
        
        assertTrue(ToolChoice.NONE.isStringType());
        assertTrue(ToolChoice.AUTO.isStringType());
        assertTrue(ToolChoice.REQUIRED.isStringType());
    }
}

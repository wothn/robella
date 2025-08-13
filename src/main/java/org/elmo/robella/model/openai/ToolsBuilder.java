package org.elmo.robella.model.openai;

/**
 * OpenAI Tools 构造工具类
 * 提供便捷的方法来创建各种工具配置
 */
public class ToolsBuilder {
    
    /**
     * 创建 Function Tool
     * 
     * @param name 函数名称
     * @param description 函数描述
     * @return Tool 对象
     */
    public static Tool function(String name, String description) {
        return function(name, description, null, false);
    }
    
    /**
     * 创建 Function Tool
     * 
     * @param name 函数名称
     * @param description 函数描述
     * @param parameters 函数参数（JSON Schema）
     * @return Tool 对象
     */
    public static Tool function(String name, String description, Object parameters) {
        return function(name, description, parameters, false);
    }
    
    /**
     * 创建 Function Tool
     * 
     * @param name 函数名称
     * @param description 函数描述
     * @param parameters 函数参数（JSON Schema）
     * @param strict 是否启用严格模式
     * @return Tool 对象
     */
    public static Tool function(String name, String description, Object parameters, boolean strict) {
        Function function = new Function();
        function.setName(name);
        function.setDescription(description);
        function.setParameters(parameters);
        function.setStrict(strict);
        
        Tool tool = new Tool();
        tool.setType("function");
        tool.setFunction(function);
        
        return tool;
    }
    
    /**
     * 创建 Custom Tool
     * 
     * @param name 工具名称
     * @return Tool 对象
     */
    public static Tool custom(String name) {
        return custom(name, null, null);
    }
    
    /**
     * 创建 Custom Tool
     * 
     * @param name 工具名称
     * @param description 工具描述
     * @return Tool 对象
     */
    public static Tool custom(String name, String description) {
        return custom(name, description, null);
    }
    
    /**
     * 创建 Custom Tool
     * 
     * @param name 工具名称
     * @param description 工具描述
     * @param format 工具格式
     * @return Tool 对象
     */
    public static Tool custom(String name, String description, CustomToolFormat format) {
        CustomTool customTool = new CustomTool();
        customTool.setName(name);
        customTool.setDescription(description);
        customTool.setFormat(format);
        
        Tool tool = new Tool();
        tool.setType("custom");
        tool.setCustom(customTool);
        
        return tool;
    }
    
    /**
     * 创建文本格式的 Custom Tool Format
     * 
     * @return CustomToolFormat 对象
     */
    public static CustomToolFormat textFormat() {
        CustomToolFormat.TextFormat textFormat = new CustomToolFormat.TextFormat();
        textFormat.setType("text");
        
        CustomToolFormat format = new CustomToolFormat();
        format.setType("text");
        format.setText(textFormat);
        
        return format;
    }
    
    /**
     * 创建语法格式的 Custom Tool Format
     * 
     * @param grammar 语法定义
     * @return CustomToolFormat 对象
     */
    public static CustomToolFormat grammarFormat(Object grammar) {
        CustomToolFormat.GrammarFormat grammarFormat = new CustomToolFormat.GrammarFormat();
        grammarFormat.setType("grammar");
        grammarFormat.setGrammar(grammar);
        
        CustomToolFormat format = new CustomToolFormat();
        format.setType("grammar");
        format.setGrammar(grammarFormat);
        
        return format;
    }
}

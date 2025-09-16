package org.elmo.robella.common;


public enum ProviderType {
    NONE("NONE", EndpointType.OPENAI),
    DEEPSEEK("DEEPSEEK", EndpointType.OPENAI),
    VOLCANOENGINE("VOLCANOENGINE", EndpointType.OPENAI),
    ZHIPU("ZHIPU", EndpointType.OPENAI),
    DASHSCOPE("DASHSCOPE", EndpointType.OPENAI),
    OPENAI("OPENAI", EndpointType.OPENAI);


    private final String name;
    private final EndpointType endpointType;

    ProviderType(String name, EndpointType endpointType) {
        this.name = name;
        this.endpointType = endpointType;
    }

    public String getName() {
        return name;
    }

    public EndpointType getEndpointType() {
        return endpointType;
    }
}

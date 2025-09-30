package org.elmo.robella.model.enums;

public enum PricingStrategyType {
    FIXED("固定价格"),
    TIERED("阶梯计费"),
    PER_REQUEST("按请求次数计费");

    private final String description;

    PricingStrategyType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
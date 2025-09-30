-- 添加阶梯计费支持

-- 1. 修改vendor_model表，添加计费策略字段
ALTER TABLE vendor_model 
ADD COLUMN IF NOT EXISTS pricing_strategy VARCHAR(50) DEFAULT 'FIXED';

-- 2. 创建pricing_tier表
CREATE TABLE IF NOT EXISTS pricing_tier (
    id BIGSERIAL PRIMARY KEY,
    vendor_model_id BIGINT NOT NULL,
    tier_number INTEGER NOT NULL,
    min_tokens BIGINT NOT NULL,
    max_tokens BIGINT,
    input_per_million_tokens DECIMAL(19, 6) NOT NULL,
    output_per_million_tokens DECIMAL(19, 6) NOT NULL,
    cached_input_price DECIMAL(19, 6) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (vendor_model_id) REFERENCES vendor_model(id) ON DELETE CASCADE
);

-- 3. 创建pricing_tier表的索引
CREATE INDEX IF NOT EXISTS idx_pricing_tier_vendor_model_id ON pricing_tier(vendor_model_id);
CREATE INDEX IF NOT EXISTS idx_pricing_tier_tier_number ON pricing_tier(tier_number);
CREATE INDEX IF NOT EXISTS idx_pricing_tier_min_tokens ON pricing_tier(min_tokens);
CREATE INDEX IF NOT EXISTS idx_pricing_tier_max_tokens ON pricing_tier(max_tokens);

-- 4. 添加表和字段注释
COMMENT ON TABLE pricing_tier IS '定价阶梯表';
COMMENT ON COLUMN pricing_tier.id IS '阶梯ID';
COMMENT ON COLUMN pricing_tier.vendor_model_id IS '供应商模型ID';
COMMENT ON COLUMN pricing_tier.tier_number IS '阶梯序号';
COMMENT ON COLUMN pricing_tier.min_tokens IS '最小令牌数';
COMMENT ON COLUMN pricing_tier.max_tokens IS '最大令牌数（可选）';
COMMENT ON COLUMN pricing_tier.input_per_million_tokens IS '每百万输入令牌价格';
COMMENT ON COLUMN pricing_tier.output_per_million_tokens IS '每百万输出令牌价格';
COMMENT ON COLUMN pricing_tier.cached_input_price IS '缓存输入价格';
COMMENT ON COLUMN pricing_tier.currency IS '货币类型';
COMMENT ON COLUMN pricing_tier.created_at IS '创建时间';
COMMENT ON COLUMN pricing_tier.updated_at IS '更新时间';

COMMENT ON COLUMN vendor_model.pricing_strategy IS '计费策略：FIXED（固定价格）、TIERED（阶梯计费）、PER_REQUEST（按请求次数计费）';




-- 6. 创建唯一约束，确保每个供应商模型的阶梯序号唯一
CREATE UNIQUE INDEX IF NOT EXISTS idx_pricing_tier_vendor_tier_unique 
ON pricing_tier(vendor_model_id, tier_number);
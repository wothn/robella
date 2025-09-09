-- V9__Recreate_tables_with_new_structure.sql
-- 删除现有表和约束
DROP TABLE IF EXISTS models CASCADE;
DROP TABLE IF EXISTS providers CASCADE;

-- 创建provider表
CREATE TABLE provider (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,          -- 如 "OpenAI", "Anthropic", "Groq"
    type VARCHAR(50) NOT NULL,                  -- 如 openai, anthropic, azure, custom
    base_url VARCHAR(512),                      -- 自定义API基地址（可选）
    api_key VARCHAR(512),                       -- api密钥
    config JSONB NOT NULL DEFAULT '{}',         -- 重试策略、超时设置等
    enabled BOOLEAN DEFAULT TRUE,               -- 是否启用该提供商
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 创建model表
CREATE TABLE model (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) UNIQUE NOT NULL,          -- 如 gpt-4o, claude-3.5-sonnet, llama-3.1-405b
    description TEXT,                           -- 模型描述
    organization VARCHAR(50),                   -- 模型创建组织，如（OpenAI）
    capabilities JSONB,                         -- 如 text, vision, audio, code, multimodal
    context_window INT,                         -- 上下文长度（token数）
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    is_published BOOLEAN DEFAULT TRUE           -- 是否在前端展示
);

-- 创建vendor_model表
CREATE TABLE vendor_model (
    id BIGSERIAL PRIMARY KEY,
    model_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    vendor_model_name VARCHAR(255) NOT NULL,    -- 该供应商下的实际模型名，如 "gpt-4o-2024-05-13"
    description TEXT,                           -- 可选：覆盖描述
    pricing JSONB NOT NULL DEFAULT '{}',        -- 价格配置
    enabled BOOLEAN DEFAULT TRUE,               -- 是否启用该映射（临时关闭某个渠道）
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(model_id, provider_id, vendor_model_name),  -- 同一模型在同供应商下不可重复映射同一实际模型
    FOREIGN KEY (model_id) REFERENCES model(id) ON DELETE CASCADE,
    FOREIGN KEY (provider_id) REFERENCES provider(id) ON DELETE CASCADE
);

-- 创建provider表索引
CREATE INDEX idx_provider_name ON provider(name);
CREATE INDEX idx_provider_type ON provider(type);
CREATE INDEX idx_provider_enabled ON provider(enabled);
CREATE INDEX idx_provider_created_at ON provider(created_at);

-- 创建model表索引
CREATE INDEX idx_model_name ON model(name);
CREATE INDEX idx_model_organization ON model(organization);
CREATE INDEX idx_model_is_published ON model(is_published);
CREATE INDEX idx_model_created_at ON model(created_at);

-- 创建vendor_model表索引
CREATE INDEX idx_vendor_model_model_id ON vendor_model(model_id);
CREATE INDEX idx_vendor_model_provider_id ON vendor_model(provider_id);
CREATE INDEX idx_vendor_model_enabled ON vendor_model(enabled);
CREATE INDEX idx_vendor_model_created_at ON vendor_model(created_at);

-- 添加注释
COMMENT ON TABLE provider IS 'AI提供商配置表';
COMMENT ON COLUMN provider.id IS '提供商ID';
COMMENT ON COLUMN provider.name IS '提供商名称';
COMMENT ON COLUMN provider.type IS '提供商类型';
COMMENT ON COLUMN provider.base_url IS '基础URL';
COMMENT ON COLUMN provider.api_key IS 'API密钥';
COMMENT ON COLUMN provider.config IS '配置信息（重试策略、超时设置等）';
COMMENT ON COLUMN provider.enabled IS '是否启用';
COMMENT ON COLUMN provider.created_at IS '创建时间';
COMMENT ON COLUMN provider.updated_at IS '更新时间';

COMMENT ON TABLE model IS 'AI模型表';
COMMENT ON COLUMN model.id IS '模型ID';
COMMENT ON COLUMN model.name IS '模型名称';
COMMENT ON COLUMN model.description IS '模型描述';
COMMENT ON COLUMN model.organization IS '模型创建组织';
COMMENT ON COLUMN model.capabilities IS '模型能力';
COMMENT ON COLUMN model.context_window IS '上下文长度';
COMMENT ON COLUMN model.is_published IS '是否在前端展示';
COMMENT ON COLUMN model.created_at IS '创建时间';
COMMENT ON COLUMN model.updated_at IS '更新时间';

COMMENT ON TABLE vendor_model IS '供应商模型映射表';
COMMENT ON COLUMN vendor_model.id IS '映射ID';
COMMENT ON COLUMN vendor_model.model_id IS '模型ID';
COMMENT ON COLUMN vendor_model.provider_id IS '提供商ID';
COMMENT ON COLUMN vendor_model.vendor_model_name IS '供应商模型名称';
COMMENT ON COLUMN vendor_model.description IS '描述';
COMMENT ON COLUMN vendor_model.pricing IS '价格配置';
COMMENT ON COLUMN vendor_model.enabled IS '是否启用';
COMMENT ON COLUMN vendor_model.created_at IS '创建时间';
COMMENT ON COLUMN vendor_model.updated_at IS '更新时间';
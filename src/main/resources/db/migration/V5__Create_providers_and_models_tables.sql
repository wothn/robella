-- V5__Create_providers_and_models_tables.sql
-- 创建AI提供商表
CREATE TABLE IF NOT EXISTS providers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL,
    api_key VARCHAR(500) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    deployment_name VARCHAR(100),
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建AI模型表
CREATE TABLE IF NOT EXISTS models (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    vendor_model VARCHAR(100) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (provider_id) REFERENCES providers(id) ON DELETE CASCADE
);

-- 创建providers表索引
CREATE INDEX IF NOT EXISTS idx_providers_name ON providers(name);
CREATE INDEX IF NOT EXISTS idx_providers_type ON providers(type);
CREATE INDEX IF NOT EXISTS idx_providers_enabled ON providers(enabled);
CREATE INDEX IF NOT EXISTS idx_providers_created_at ON providers(created_at);

-- 创建models表索引
CREATE INDEX IF NOT EXISTS idx_models_provider_id ON models(provider_id);
CREATE INDEX IF NOT EXISTS idx_models_name ON models(name);
CREATE INDEX IF NOT EXISTS idx_models_enabled ON models(enabled);
CREATE INDEX IF NOT EXISTS idx_models_created_at ON models(created_at);

-- 添加注释（使用DO块避免字段不存在时的错误）
DO $$
BEGIN
    COMMENT ON TABLE providers IS 'AI提供商配置表';
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'providers' AND column_name = 'id') THEN
        COMMENT ON COLUMN providers.id IS '提供商ID';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'providers' AND column_name = 'name') THEN
        COMMENT ON COLUMN providers.name IS '提供商名称';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'providers' AND column_name = 'type') THEN
        COMMENT ON COLUMN providers.type IS '提供商类型';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'providers' AND column_name = 'api_key') THEN
        COMMENT ON COLUMN providers.api_key IS 'API密钥';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'providers' AND column_name = 'base_url') THEN
        COMMENT ON COLUMN providers.base_url IS '基础URL';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'providers' AND column_name = 'deployment_name') THEN
        COMMENT ON COLUMN providers.deployment_name IS '部署名称';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'providers' AND column_name = 'enabled') THEN
        COMMENT ON COLUMN providers.enabled IS '是否启用';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'providers' AND column_name = 'created_at') THEN
        COMMENT ON COLUMN providers.created_at IS '创建时间';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'providers' AND column_name = 'updated_at') THEN
        COMMENT ON COLUMN providers.updated_at IS '更新时间';
    END IF;
END $$;

-- 添加models表注释
DO $$
BEGIN
    COMMENT ON TABLE models IS 'AI模型配置表';
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'models' AND column_name = 'id') THEN
        COMMENT ON COLUMN models.id IS '模型ID';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'models' AND column_name = 'provider_id') THEN
        COMMENT ON COLUMN models.provider_id IS '提供商ID';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'models' AND column_name = 'name') THEN
        COMMENT ON COLUMN models.name IS '模型名称';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'models' AND column_name = 'vendor_model') THEN
        COMMENT ON COLUMN models.vendor_model IS '供应商模型名称';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'models' AND column_name = 'enabled') THEN
        COMMENT ON COLUMN models.enabled IS '是否启用';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'models' AND column_name = 'created_at') THEN
        COMMENT ON COLUMN models.created_at IS '创建时间';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'models' AND column_name = 'updated_at') THEN
        COMMENT ON COLUMN models.updated_at IS '更新时间';
    END IF;
END $$;
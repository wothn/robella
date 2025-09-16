-- 用户表创建脚本
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) DEFAULT NULL,
    display_name VARCHAR(100),
    avatar VARCHAR(500),
    active BOOLEAN DEFAULT TRUE,
    role INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    github_id VARCHAR(100)
);

-- AI提供商表
CREATE TABLE IF NOT EXISTS provider (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(50),
    base_url VARCHAR(500),
    api_key VARCHAR(500),
    config TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- AI模型表
CREATE TABLE IF NOT EXISTS model (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    organization VARCHAR(100),
    capabilities JSONB,
    context_window INTEGER,
    published BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 供应商模型表
CREATE TABLE IF NOT EXISTS vendor_model (
    id BIGSERIAL PRIMARY KEY,
    model_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    vendor_model_name VARCHAR(100) NOT NULL,
    provider_type VARCHAR(50),
    description TEXT,
    input_per_million_tokens DECIMAL(19, 6),
    output_per_million_tokens DECIMAL(19, 6),
    currency VARCHAR(10),
    cached_input_price DECIMAL(19, 6),
    cached_output_price DECIMAL(19, 6),
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (model_id) REFERENCES model(id) ON DELETE CASCADE,
    FOREIGN KEY (provider_id) REFERENCES provider(id) ON DELETE CASCADE
);

-- API密钥表
CREATE TABLE IF NOT EXISTS api_key (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    key_hash VARCHAR(255) NOT NULL,
    key_prefix VARCHAR(16) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    daily_limit INTEGER DEFAULT 0,
    monthly_limit INTEGER DEFAULT 0,
    rate_limit INTEGER DEFAULT 60,
    active BOOLEAN DEFAULT TRUE,
    last_used_at TIMESTAMP,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_active ON users(active);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);
CREATE INDEX IF NOT EXISTS idx_users_github_id ON users(github_id);

-- provider表索引
CREATE INDEX IF NOT EXISTS idx_provider_name ON provider(name);
CREATE INDEX IF NOT EXISTS idx_provider_type ON provider(type);
CREATE INDEX IF NOT EXISTS idx_provider_enabled ON provider(enabled);
CREATE INDEX IF NOT EXISTS idx_provider_created_at ON provider(created_at);

-- model表索引
CREATE INDEX IF NOT EXISTS idx_model_name ON model(name);
CREATE INDEX IF NOT EXISTS idx_model_organization ON model(organization);
CREATE INDEX IF NOT EXISTS idx_model_published ON model(published);
CREATE INDEX IF NOT EXISTS idx_model_created_at ON model(created_at);

-- vendor_model表索引
CREATE INDEX IF NOT EXISTS idx_vendor_model_model_id ON vendor_model(model_id);
CREATE INDEX IF NOT EXISTS idx_vendor_model_provider_id ON vendor_model(provider_id);
CREATE INDEX IF NOT EXISTS idx_vendor_model_vendor_model_name ON vendor_model(vendor_model_name);
CREATE INDEX IF NOT EXISTS idx_vendor_model_enabled ON vendor_model(enabled);
CREATE INDEX IF NOT EXISTS idx_vendor_model_created_at ON vendor_model(created_at);

-- api_key表索引
CREATE INDEX IF NOT EXISTS idx_api_key_user_id ON api_key(user_id);
CREATE INDEX IF NOT EXISTS idx_api_key_key_prefix ON api_key(key_prefix);
CREATE INDEX IF NOT EXISTS idx_api_key_active ON api_key(active);
CREATE INDEX IF NOT EXISTS idx_api_key_expires_at ON api_key(expires_at);
CREATE INDEX IF NOT EXISTS idx_api_key_created_at ON api_key(created_at);

-- 添加注释
COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.id IS '用户ID';
COMMENT ON COLUMN users.username IS '用户名';
COMMENT ON COLUMN users.email IS '邮箱';
COMMENT ON COLUMN users.password IS '密码';
COMMENT ON COLUMN users.display_name IS '显示名称';
COMMENT ON COLUMN users.avatar IS '头像URL';
COMMENT ON COLUMN users.active IS '是否活跃';
COMMENT ON COLUMN users.role IS '角色';
COMMENT ON COLUMN users.created_at IS '创建时间';
COMMENT ON COLUMN users.updated_at IS '更新时间';
COMMENT ON COLUMN users.last_login_at IS '最后登录时间';
COMMENT ON COLUMN users.github_id IS 'GitHub用户ID';

-- provider表注释
COMMENT ON TABLE provider IS 'AI提供商配置表';
COMMENT ON COLUMN provider.id IS '提供商ID';
COMMENT ON COLUMN provider.name IS '提供商名称';
COMMENT ON COLUMN provider.type IS '提供商类型';
COMMENT ON COLUMN provider.base_url IS '基础URL';
COMMENT ON COLUMN provider.api_key IS 'API密钥';
COMMENT ON COLUMN provider.config IS '配置信息';
COMMENT ON COLUMN provider.enabled IS '是否启用';
COMMENT ON COLUMN provider.created_at IS '创建时间';
COMMENT ON COLUMN provider.updated_at IS '更新时间';

-- model表注释
COMMENT ON TABLE model IS 'AI模型表';
COMMENT ON COLUMN model.id IS '模型ID';
COMMENT ON COLUMN model.name IS '模型名称';
COMMENT ON COLUMN model.description IS '模型描述';
COMMENT ON COLUMN model.organization IS '组织';
COMMENT ON COLUMN model.capabilities IS '能力';
COMMENT ON COLUMN model.context_window IS '上下文窗口';
COMMENT ON COLUMN model.published IS '是否发布';
COMMENT ON COLUMN model.created_at IS '创建时间';
COMMENT ON COLUMN model.updated_at IS '更新时间';

-- vendor_model表注释
COMMENT ON TABLE vendor_model IS '供应商模型表';
COMMENT ON COLUMN vendor_model.id IS '供应商模型ID';
COMMENT ON COLUMN vendor_model.model_id IS '模型ID';
COMMENT ON COLUMN vendor_model.provider_id IS '提供商ID';
COMMENT ON COLUMN vendor_model.vendor_model_name IS '供应商模型名称';
COMMENT ON COLUMN vendor_model.description IS '描述';
COMMENT ON COLUMN vendor_model.input_per_million_tokens IS '每百万输入令牌价格';
    COMMENT ON COLUMN vendor_model.output_per_million_tokens IS '每百万输出令牌价格';
    COMMENT ON COLUMN vendor_model.currency IS '货币类型';
    COMMENT ON COLUMN vendor_model.cached_input_price IS '缓存输入价格';
    COMMENT ON COLUMN vendor_model.cached_output_price IS '缓存输出价格';
COMMENT ON COLUMN vendor_model.enabled IS '是否启用';
COMMENT ON COLUMN vendor_model.created_at IS '创建时间';
COMMENT ON COLUMN vendor_model.updated_at IS '更新时间';

-- api_key表注释
COMMENT ON TABLE api_key IS 'API密钥表';
COMMENT ON COLUMN api_key.id IS 'API密钥ID';
COMMENT ON COLUMN api_key.user_id IS '用户ID';
COMMENT ON COLUMN api_key.key_hash IS '密钥哈希值';
COMMENT ON COLUMN api_key.key_prefix IS '密钥前缀';
COMMENT ON COLUMN api_key.name IS '密钥名称';
COMMENT ON COLUMN api_key.description IS '密钥描述';
COMMENT ON COLUMN api_key.daily_limit IS '日限制(0为无限制)';
COMMENT ON COLUMN api_key.monthly_limit IS '月限制(0为无限制)';
COMMENT ON COLUMN api_key.rate_limit IS '速率限制(每分钟请求数)';
COMMENT ON COLUMN api_key.active IS '是否启用';
COMMENT ON COLUMN api_key.last_used_at IS '最后使用时间';
COMMENT ON COLUMN api_key.expires_at IS '过期时间';
COMMENT ON COLUMN api_key.created_at IS '创建时间';
COMMENT ON COLUMN api_key.updated_at IS '更新时间';
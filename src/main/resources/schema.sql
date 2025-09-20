-- 用户表创建脚本
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) DEFAULT NULL,
    display_name VARCHAR(100),
    avatar VARCHAR(500),
    active BOOLEAN DEFAULT TRUE,
    role VARCHAR(20) DEFAULT 'USER',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP WITH TIME ZONE,
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
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- AI模型表
CREATE TABLE IF NOT EXISTS model (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    model_key VARCHAR(200) NOT NULL,
    description TEXT,
    organization VARCHAR(100),
    capabilities TEXT,
    context_window INTEGER,
    published BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 供应商模型表
CREATE TABLE IF NOT EXISTS vendor_model (
    id BIGSERIAL PRIMARY KEY,
    model_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    vendor_model_name VARCHAR(100) NOT NULL,
    model_key VARCHAR(200) NOT NULL,
    provider_type VARCHAR(50),
    description TEXT,
    input_per_million_tokens DECIMAL(19, 6),
    output_per_million_tokens DECIMAL(19, 6),
    currency VARCHAR(10),
    cached_input_price DECIMAL(19, 6),
    cached_output_price DECIMAL(19, 6),
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
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
    last_used_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
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
CREATE INDEX IF NOT EXISTS idx_model_model_key ON model(model_key);
CREATE INDEX IF NOT EXISTS idx_model_organization ON model(organization);
CREATE INDEX IF NOT EXISTS idx_model_published ON model(published);
CREATE INDEX IF NOT EXISTS idx_model_created_at ON model(created_at);

-- vendor_model表索引
CREATE INDEX IF NOT EXISTS idx_vendor_model_model_id ON vendor_model(model_id);
CREATE INDEX IF NOT EXISTS idx_vendor_model_provider_id ON vendor_model(provider_id);
CREATE INDEX IF NOT EXISTS idx_vendor_model_vendor_model_name ON vendor_model(vendor_model_name);
CREATE INDEX IF NOT EXISTS idx_vendor_model_model_key ON vendor_model(model_key);
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
COMMENT ON COLUMN model.model_key IS '模型调用标识';
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
COMMENT ON COLUMN vendor_model.model_key IS '模型调用标识';
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

-- 请求日志表
CREATE TABLE IF NOT EXISTS request_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    api_key_id BIGINT,
    model_name VARCHAR(100),
    vendor_model_name VARCHAR(100),
    provider_id BIGINT,
    endpoint_type VARCHAR(20),
    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    total_tokens INTEGER,
    token_source VARCHAR(20),
    calculation_method VARCHAR(20),
    input_cost DECIMAL(19, 6),
    output_cost DECIMAL(19, 6),
    total_cost DECIMAL(19, 6),
    currency VARCHAR(10),
    duration_ms INTEGER,
    first_token_latency_ms INTEGER,
    tokens_per_second DECIMAL(19, 6),
    is_stream BOOLEAN,
    is_success BOOLEAN,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (api_key_id) REFERENCES api_key(id) ON DELETE SET NULL,
    FOREIGN KEY (provider_id) REFERENCES provider(id) ON DELETE SET NULL
);

-- request_log表索引
CREATE INDEX IF NOT EXISTS idx_request_log_user_id ON request_log(user_id);
CREATE INDEX IF NOT EXISTS idx_request_log_api_key_id ON request_log(api_key_id);
CREATE INDEX IF NOT EXISTS idx_request_log_provider_id ON request_log(provider_id);
CREATE INDEX IF NOT EXISTS idx_request_log_model_name ON request_log(model_name);
CREATE INDEX IF NOT EXISTS idx_request_log_created_at ON request_log(created_at);
CREATE INDEX IF NOT EXISTS idx_request_log_is_success ON request_log(is_success);
CREATE INDEX IF NOT EXISTS idx_request_log_endpoint_type ON request_log(endpoint_type);

-- request_log表注释
COMMENT ON TABLE request_log IS 'API请求日志表';
COMMENT ON COLUMN request_log.id IS '日志ID';
COMMENT ON COLUMN request_log.user_id IS '用户ID';
COMMENT ON COLUMN request_log.api_key_id IS 'API密钥ID';
COMMENT ON COLUMN request_log.model_name IS '请求模型名称';
COMMENT ON COLUMN request_log.vendor_model_name IS '供应商模型名称';
COMMENT ON COLUMN request_log.provider_id IS '提供商ID';
COMMENT ON COLUMN request_log.endpoint_type IS '端点类型';
COMMENT ON COLUMN request_log.prompt_tokens IS '输入令牌数';
COMMENT ON COLUMN request_log.completion_tokens IS '输出令牌数';
COMMENT ON COLUMN request_log.total_tokens IS '总令牌数';
COMMENT ON COLUMN request_log.token_source IS '令牌来源';
COMMENT ON COLUMN request_log.calculation_method IS '计算方法';
COMMENT ON COLUMN request_log.input_cost IS '输入成本';
COMMENT ON COLUMN request_log.output_cost IS '输出成本';
COMMENT ON COLUMN request_log.total_cost IS '总成本';
COMMENT ON COLUMN request_log.currency IS '货币类型';
COMMENT ON COLUMN request_log.duration_ms IS '请求持续时间(毫秒)';
COMMENT ON COLUMN request_log.first_token_latency_ms IS '首令牌延迟(毫秒)';
COMMENT ON COLUMN request_log.tokens_per_second IS '每秒令牌数';
COMMENT ON COLUMN request_log.is_stream IS '是否流式请求';
COMMENT ON COLUMN request_log.is_success IS '是否成功';
COMMENT ON COLUMN request_log.created_at IS '创建时间';
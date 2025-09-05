-- 用户表创建脚本
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) DEFAULT NULL,
    full_name VARCHAR(100),
    avatar VARCHAR(500),
    phone VARCHAR(20),
    active BOOLEAN DEFAULT TRUE,
    role VARCHAR(50) DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    email_verified VARCHAR(5) DEFAULT 'false',
    phone_verified VARCHAR(5) DEFAULT 'false',
    github_id VARCHAR(100),
    provider VARCHAR(50) DEFAULT 'local',
    provider_id VARCHAR(100)
);

-- AI提供商表
CREATE TABLE IF NOT EXISTS providers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL,
    api_key VARCHAR(500) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    deployment_name VARCHAR(100),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- AI模型表
CREATE TABLE IF NOT EXISTS models (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    vendor_model VARCHAR(100) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (provider_id) REFERENCES providers(id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_active ON users(active);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);

-- providers表索引
CREATE INDEX IF NOT EXISTS idx_providers_name ON providers(name);
CREATE INDEX IF NOT EXISTS idx_providers_type ON providers(type);
CREATE INDEX IF NOT EXISTS idx_providers_active ON providers(active);
CREATE INDEX IF NOT EXISTS idx_providers_created_at ON providers(created_at);

-- models表索引
CREATE INDEX IF NOT EXISTS idx_models_provider_id ON models(provider_id);
CREATE INDEX IF NOT EXISTS idx_models_name ON models(name);
CREATE INDEX IF NOT EXISTS idx_models_active ON models(active);
CREATE INDEX IF NOT EXISTS idx_models_created_at ON models(created_at);

-- 添加注释
COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.id IS '用户ID';
COMMENT ON COLUMN users.username IS '用户名';
COMMENT ON COLUMN users.email IS '邮箱';
COMMENT ON COLUMN users.password IS '密码';
COMMENT ON COLUMN users.full_name IS '全名';
COMMENT ON COLUMN users.avatar IS '头像URL';
COMMENT ON COLUMN users.phone IS '手机号';
COMMENT ON COLUMN users.active IS '是否活跃';
COMMENT ON COLUMN users.role IS '角色';
COMMENT ON COLUMN users.created_at IS '创建时间';
COMMENT ON COLUMN users.updated_at IS '更新时间';
COMMENT ON COLUMN users.last_login_at IS '最后登录时间';
COMMENT ON COLUMN users.email_verified IS '邮箱是否验证';
COMMENT ON COLUMN users.phone_verified IS '手机是否验证';
COMMENT ON COLUMN users.github_id IS 'GitHub用户ID';
COMMENT ON COLUMN users.provider IS '登录提供商';
COMMENT ON COLUMN users.provider_id IS '提供商用户ID';

-- providers表注释
COMMENT ON TABLE providers IS 'AI提供商配置表';
COMMENT ON COLUMN providers.id IS '提供商ID';
COMMENT ON COLUMN providers.name IS '提供商名称';
COMMENT ON COLUMN providers.type IS '提供商类型';
COMMENT ON COLUMN providers.api_key IS 'API密钥';
COMMENT ON COLUMN providers.base_url IS '基础URL';
COMMENT ON COLUMN providers.deployment_name IS '部署名称';
COMMENT ON COLUMN providers.active IS '是否启用';
COMMENT ON COLUMN providers.created_at IS '创建时间';
COMMENT ON COLUMN providers.updated_at IS '更新时间';

-- models表注释
COMMENT ON TABLE models IS 'AI模型配置表';
COMMENT ON COLUMN models.id IS '模型ID';
COMMENT ON COLUMN models.provider_id IS '提供商ID';
COMMENT ON COLUMN models.name IS '模型名称';
COMMENT ON COLUMN models.vendor_model IS '供应商模型名称';
COMMENT ON COLUMN models.active IS '是否启用';
COMMENT ON COLUMN models.created_at IS '创建时间';
COMMENT ON COLUMN models.updated_at IS '更新时间';
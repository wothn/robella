-- 创建数据库schema (SQLite version)
-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    display_name VARCHAR(255),
    avatar VARCHAR(500),
    credits REAL DEFAULT 0.00,
    active INTEGER DEFAULT 1,
    role VARCHAR(50) DEFAULT 'USER',
    created_at TEXT DEFAULT (datetime('now')),
    updated_at TEXT DEFAULT (datetime('now')),
    last_login_at TEXT,
    github_id INTEGER UNIQUE
);

-- AI服务提供商表
CREATE TABLE IF NOT EXISTS provider (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(255) NOT NULL UNIQUE,
    endpoint_type VARCHAR(50) NOT NULL,
    provider_type VARCHAR(50) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    api_key VARCHAR(500) NOT NULL,
    config TEXT,
    enabled INTEGER DEFAULT 1,
    created_at TEXT DEFAULT (datetime('now')),
    updated_at TEXT DEFAULT (datetime('now'))
);

-- AI模型表
CREATE TABLE IF NOT EXISTS model (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(255) NOT NULL UNIQUE,
    model_key VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    organization VARCHAR(255),
    capabilities TEXT,
    context_window INTEGER DEFAULT 4096,
    published INTEGER DEFAULT 1,
    created_at TEXT DEFAULT (datetime('now')),
    updated_at TEXT DEFAULT (datetime('now'))
);

-- 供应商模型表（连接Provider和Model）
CREATE TABLE IF NOT EXISTS vendor_model (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    model_id INTEGER NOT NULL REFERENCES model(id) ON DELETE CASCADE,
    model_key VARCHAR(255) NOT NULL,
    provider_id INTEGER NOT NULL REFERENCES provider(id) ON DELETE CASCADE,
    vendor_model_name VARCHAR(255) NOT NULL,
    vendor_model_key VARCHAR(255) NOT NULL,
    provider_type VARCHAR(50) NOT NULL,
    description TEXT,
    input_per_million_tokens REAL DEFAULT 0.000000,
    output_per_million_tokens REAL DEFAULT 0.000000,
    cached_per_million_tokens REAL DEFAULT 0.000000,
    pricing_strategy VARCHAR(50) DEFAULT 'PER_TOKEN',
    enabled INTEGER DEFAULT 1,
    weight INTEGER DEFAULT 100,
    created_at TEXT DEFAULT (datetime('now')),
    updated_at TEXT DEFAULT (datetime('now')),
    UNIQUE(provider_id, vendor_model_key)
);

-- API密钥表
CREATE TABLE IF NOT EXISTS api_key (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    key_hash VARCHAR(255) NOT NULL UNIQUE,
    key_prefix VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    daily_limit INTEGER,
    monthly_limit INTEGER,
    rate_limit INTEGER,
    active INTEGER DEFAULT 1,
    last_used_at TEXT,
    expires_at TEXT,
    created_at TEXT DEFAULT (datetime('now')),
    updated_at TEXT DEFAULT (datetime('now'))
);

-- 请求日志表
CREATE TABLE IF NOT EXISTS request_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    request_id VARCHAR(255) NOT NULL UNIQUE,
    user_id INTEGER REFERENCES users(id),
    api_key_id INTEGER REFERENCES api_key(id),
    model_key VARCHAR(255),
    vendor_model_key VARCHAR(255),
    provider_id INTEGER REFERENCES provider(id),
    endpoint_type VARCHAR(50),
    prompt_tokens INTEGER DEFAULT 0,
    cached_tokens INTEGER DEFAULT 0,
    completion_tokens INTEGER DEFAULT 0,
    total_tokens INTEGER DEFAULT 0,
    input_cost REAL DEFAULT 0.000000,
    output_cost REAL DEFAULT 0.000000,
    cached_cost REAL DEFAULT 0.000000,
    total_cost REAL DEFAULT 0.000000,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    response_time_ms INTEGER,
    created_at TEXT DEFAULT (datetime('now'))
);

-- 定价层级表
CREATE TABLE IF NOT EXISTS pricing_tier (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    vendor_model_id INTEGER NOT NULL REFERENCES vendor_model(id) ON DELETE CASCADE,
    tier_number INTEGER NOT NULL,
    min_tokens INTEGER NOT NULL,
    max_tokens INTEGER,
    input_per_million_tokens REAL NOT NULL,
    output_per_million_tokens REAL NOT NULL,
    cached_per_million_tokens REAL DEFAULT 0.000000,
    created_at TEXT DEFAULT (datetime('now')),
    updated_at TEXT DEFAULT (datetime('now')),
    UNIQUE(vendor_model_id, tier_number)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_github_id ON users(github_id);

CREATE INDEX IF NOT EXISTS idx_provider_name ON provider(name);
CREATE INDEX IF NOT EXISTS idx_provider_enabled ON provider(enabled);

CREATE INDEX IF NOT EXISTS idx_model_name ON model(name);
CREATE INDEX IF NOT EXISTS idx_model_key ON model(model_key);
CREATE INDEX IF NOT EXISTS idx_model_published ON model(published);

CREATE INDEX IF NOT EXISTS idx_vendor_model_model_id ON vendor_model(model_id);
CREATE INDEX IF NOT EXISTS idx_vendor_model_provider_id ON vendor_model(provider_id);
CREATE INDEX IF NOT EXISTS idx_vendor_model_key ON vendor_model(vendor_model_key);
CREATE INDEX IF NOT EXISTS idx_vendor_model_enabled ON vendor_model(enabled);

CREATE INDEX IF NOT EXISTS idx_api_key_user_id ON api_key(user_id);
CREATE INDEX IF NOT EXISTS idx_api_key_hash ON api_key(key_hash);
CREATE INDEX IF NOT EXISTS idx_api_key_active ON api_key(active);

CREATE INDEX IF NOT EXISTS idx_request_log_user_id ON request_log(user_id);
CREATE INDEX IF NOT EXISTS idx_request_log_api_key_id ON request_log(api_key_id);
CREATE INDEX IF NOT EXISTS idx_request_log_model_key ON request_log(model_key);
CREATE INDEX IF NOT EXISTS idx_request_log_created_at ON request_log(created_at);
CREATE INDEX IF NOT EXISTS idx_request_log_status ON request_log(status);

CREATE INDEX IF NOT EXISTS idx_pricing_tier_vendor_model_id ON pricing_tier(vendor_model_id);

-- 插入默认 ROOT 用户
-- 用户名: root
-- 密码: root123 (BCrypt 加密)
-- 邮箱: root@robella.local
-- 注意：使用 ISO 8601 格式存储时间
INSERT INTO users (username, email, display_name, role, password, active, created_at, updated_at)
VALUES ('root', 'root@robella.local', 'Root Administrator', 'ROOT', '$2a$10$Qidr9Pv4Fjk9n40iqa2lHeXehvxwEIKGx/GfREMef/V61fdSKuuSa', 1, datetime('now'), datetime('now'));

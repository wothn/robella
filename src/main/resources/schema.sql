-- 用户表创建脚本
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255),
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

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_active ON users(active);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);

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
-- V8__Update_user_entity_fields.sql
-- 更新用户表字段以匹配User实体类

-- 1. 添加display_name字段并迁移full_name数据
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'display_name') THEN
        ALTER TABLE users ADD COLUMN display_name VARCHAR(100);
        COMMENT ON COLUMN users.display_name IS '显示名称';
    END IF;
END $$;

-- 将现有的full_name数据复制到display_name字段
UPDATE users SET display_name = full_name WHERE display_name IS NULL AND full_name IS NOT NULL;

-- 2. 添加role_integer字段
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'role_integer') THEN
        ALTER TABLE users ADD COLUMN role_integer INTEGER;
        COMMENT ON COLUMN users.role_integer IS '角色（整数类型）';
    END IF;
END $$;

-- 将现有的role字符串转换为整数
UPDATE users SET role_integer = CASE 
    WHEN role = 'ADMIN' THEN 1
    WHEN role = 'USER' THEN 0
    ELSE 0
END WHERE role_integer IS NULL;

-- 确保所有现有记录都有role_integer值
UPDATE users SET role_integer = 0 WHERE role_integer IS NULL;

-- 删除旧的role字段
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'role') THEN
        ALTER TABLE users DROP COLUMN role;
    END IF;
END $$;

-- 将role_integer重命名为role
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'role_integer') THEN
        ALTER TABLE users RENAME COLUMN role_integer TO role;
    END IF;
END $$;

-- 3. 删除实体类中不存在的字段
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'phone') THEN
        ALTER TABLE users DROP COLUMN phone;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'full_name') THEN
        ALTER TABLE users DROP COLUMN full_name;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'email_verified') THEN
        ALTER TABLE users DROP COLUMN email_verified;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'phone_verified') THEN
        ALTER TABLE users DROP COLUMN phone_verified;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'provider') THEN
        ALTER TABLE users DROP COLUMN provider;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'provider_id') THEN
        ALTER TABLE users DROP COLUMN provider_id;
    END IF;
END $$;

-- 为display_name字段创建索引
CREATE INDEX IF NOT EXISTS idx_users_display_name ON users(display_name);

-- 为role字段创建索引
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- 更新字段注释
COMMENT ON COLUMN users.display_name IS '显示名称';
COMMENT ON COLUMN users.role IS '角色（整数类型：0-USER，1-ADMIN）';
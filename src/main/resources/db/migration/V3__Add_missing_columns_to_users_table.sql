-- V3__Add_missing_columns_to_users_table.sql
-- 为现有的users表添加缺失的字段

-- 添加github_id字段（如果不存在）
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'github_id') THEN
        ALTER TABLE users ADD COLUMN github_id VARCHAR(100);
        COMMENT ON COLUMN users.github_id IS 'GitHub用户ID';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'provider') THEN
        ALTER TABLE users ADD COLUMN provider VARCHAR(50) DEFAULT 'local';
        COMMENT ON COLUMN users.provider IS '登录提供商';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'provider_id') THEN
        ALTER TABLE users ADD COLUMN provider_id VARCHAR(100);
        COMMENT ON COLUMN users.provider_id IS '提供商用户ID';
    END IF;
END $$;
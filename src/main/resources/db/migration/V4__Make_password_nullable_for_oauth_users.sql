-- V4__Make_password_nullable_for_oauth_users.sql
-- 移除password字段的NOT NULL约束，以支持OAuth用户

-- 移除password字段的NOT NULL约束
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

-- 添加注释说明
COMMENT ON COLUMN users.password IS '密码（OAuth用户可以为空）';

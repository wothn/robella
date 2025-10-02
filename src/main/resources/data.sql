-- 插入基础数据
-- 注意：这些数据主要用于生产环境初始化，开发环境可能不需要

-- 插入默认管理员用户（密码需要在应用中加密）
INSERT INTO users (username, email, display_name, role, password, active) 
VALUES ('admin', 'admin@robella.ai', 'Administrator', 'ROOT', '$2a$10$CWIzyYYhmvn27Gbb5czPPeiPXmhIw0yZ1OteESBdV0D/0PGTEJjva', true)
ON CONFLICT (username) DO NOTHING;


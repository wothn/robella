-- 插入测试用户数据（使用ON CONFLICT DO NOTHING避免重复插入）
INSERT INTO users (username, email, password, full_name, avatar, role, active, email_verified, phone_verified) VALUES 
('admin', 'admin@robella.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVYITi', '系统管理员', 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin', 'ADMIN', true, 'true', 'false'),
('user1', 'user1@robella.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVYITi', '普通用户1', 'https://api.dicebear.com/7.x/avataaars/svg?seed=user1', 'USER', true, 'true', 'false'),
('user2', 'user2@robella.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVYITi', '普通用户2', null, 'USER', true, 'false', 'false'),
('demo', 'demo@robella.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVYITi', '演示用户', 'https://api.dicebear.com/7.x/avataaars/svg?seed=demo', 'USER', true, 'true', 'true')
ON CONFLICT (username) DO NOTHING;

-- 密码说明：以上密码都是 'password123' 的BCrypt加密结果
-- 在实际生产环境中，请使用更复杂的密码
-- ON CONFLICT (username) DO NOTHING 表示如果用户名已存在，则跳过插入，避免唯一约束冲突
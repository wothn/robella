-- 插入测试用户数据
INSERT INTO users (username, email, password, full_name, role, active, email_verified, phone_verified) VALUES 
('admin', 'admin@robella.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVYITi', '系统管理员', 'ADMIN', true, 'true', 'false'),
('user1', 'user1@robella.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVYITi', '普通用户1', 'USER', true, 'true', 'false'),
('user2', 'user2@robella.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVYITi', '普通用户2', 'USER', true, 'false', 'false'),
('demo', 'demo@robella.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVYITi', '演示用户', 'USER', true, 'true', 'true');

-- 密码说明：以上密码都是 'password123' 的BCrypt加密结果
-- 在实际生产环境中，请使用更复杂的密码
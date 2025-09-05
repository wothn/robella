-- 插入测试用户数据（使用ON CONFLICT DO NOTHING避免重复插入）
INSERT INTO users (username, email, password, full_name, avatar, role, active, email_verified, phone_verified) VALUES 
('admin', 'admin@robella.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVYITi', '系统管理员', 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin', 'ADMIN', true, 'true', 'false'),
('user1', 'user1@robella.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVYITi', '普通用户1', 'https://api.dicebear.com/7.x/avataaars/svg?seed=user1', 'USER', true, 'true', 'false'),
('user2', 'user2@robella.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVYITi', '普通用户2', null, 'USER', true, 'false', 'false'),
('demo', 'demo@robella.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVYITi', '演示用户', 'https://api.dicebear.com/7.x/avataaars/svg?seed=demo', 'USER', true, 'true', 'true')
ON CONFLICT (username) DO NOTHING;

-- 插入AI提供商测试数据
INSERT INTO providers (name, type, api_key, base_url, deployment_name, active) VALUES 
('aihubmix', 'OpenAI', 'your-api-key-here', 'https://aihubmix.com/v1', null, true),
('openai', 'OpenAI', 'your-openai-api-key', 'https://api.openai.com/v1', null, true),
('anthropic', 'Anthropic', 'your-anthropic-api-key', 'https://api.anthropic.com', null, true)
ON CONFLICT (name) DO NOTHING;

-- 插入AI模型测试数据
-- 注意：需要先获取providers的ID，这里假设ID是连续的
INSERT INTO models (provider_id, name, vendor_model, active) VALUES 
(1, 'GLM-4.5', 'GLM-4.5', true),
(1, 'gpt-5-nano', 'gpt-5-nano', true),
(2, 'gpt-4', 'gpt-4', true),
(2, 'gpt-4-turbo', 'gpt-4-turbo', true),
(2, 'gpt-3.5-turbo', 'gpt-3.5-turbo', true),
(3, 'claude-3-opus', 'claude-3-opus-20240229', true),
(3, 'claude-3-sonnet', 'claude-3-sonnet-20240229', true),
(3, 'claude-3-haiku', 'claude-3-haiku-20240307', true)
ON CONFLICT DO NOTHING;

-- 密码说明：以上密码都是 'password123' 的BCrypt加密结果
-- 在实际生产环境中，请使用更复杂的密码
-- ON CONFLICT (username) DO NOTHING 表示如果用户名已存在，则跳过插入，避免唯一约束冲突
-- AI提供商的API密钥需要替换为真实的密钥
-- 模型数据中的provider_id需要根据实际插入的providers表ID进行调整
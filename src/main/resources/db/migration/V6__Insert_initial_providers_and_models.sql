-- V6__Insert_initial_providers_and_models.sql
-- 插入初始AI提供商数据
INSERT INTO providers (name, type, api_key, base_url, deployment_name, active) VALUES
('OpenAI', 'OPENAI', 'your-openai-api-key', 'https://api.openai.com/v1', NULL, TRUE),
('Anthropic', 'ANTHROPIC', 'your-anthropic-api-key', 'https://api.anthropic.com', NULL, TRUE),
('DeepSeek', 'OPENAI', 'your-deepseek-api-key', 'https://api.deepseek.com/v1', NULL, TRUE),
('Azure OpenAI', 'OPENAI', 'your-azure-openai-api-key', 'https://your-resource.openai.azure.com/openai/deployments', 'gpt-4', TRUE),
('ModelScope', 'OPENAI', 'your-modelscope-api-key', 'https://api.modelscope.cn/v1', NULL, TRUE),
('AIHubMix', 'OPENAI', 'your-aihubmix-api-key', 'https://aihubmix.com/v1', NULL, TRUE);

-- 插入初始AI模型数据
-- OpenAI models
INSERT INTO models (provider_id, name, vendor_model, active) VALUES
(1, 'gpt-4', 'gpt-4', TRUE),
(1, 'gpt-4-turbo', 'gpt-4-turbo', TRUE),
(1, 'gpt-4o', 'gpt-4o', TRUE),
(1, 'gpt-4o-mini', 'gpt-4o-mini', TRUE);

-- Anthropic models
INSERT INTO models (provider_id, name, vendor_model, active) VALUES
(2, 'claude-3-opus', 'claude-3-opus-20240229', TRUE),
(2, 'claude-3-sonnet', 'claude-3-sonnet-20240229', TRUE),
(2, 'claude-3-haiku', 'claude-3-haiku-20240307', TRUE),
(2, 'claude-3-5-sonnet', 'claude-3-5-sonnet-20241022', TRUE);

-- DeepSeek models
INSERT INTO models (provider_id, name, vendor_model, active) VALUES
(3, 'deepseek-chat', 'deepseek-chat', TRUE),
(3, 'deepseek-coder', 'deepseek-coder', TRUE);

-- Azure OpenAI models
INSERT INTO models (provider_id, name, vendor_model, active) VALUES
(4, 'gpt-4', 'gpt-4', TRUE),
(4, 'gpt-35-turbo', 'gpt-35-turbo', TRUE);

-- ModelScope models
INSERT INTO models (provider_id, name, vendor_model, active) VALUES
(5, 'qwen-turbo', 'qwen-turbo', TRUE),
(5, 'qwen-plus', 'qwen-plus', TRUE),
(5, 'qwen-max', 'qwen-max', TRUE);

-- AIHubMix models
INSERT INTO models (provider_id, name, vendor_model, active) VALUES
(6, 'gpt-4', 'gpt-4', TRUE),
(6, 'gpt-3.5-turbo', 'gpt-3.5-turbo', TRUE),
(6, 'claude-3-sonnet', 'claude-3-sonnet', TRUE);
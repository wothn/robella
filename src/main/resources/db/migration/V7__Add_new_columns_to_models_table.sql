-- V7__Add_new_columns_to_models_table.sql
-- 为models表添加新字段

-- 添加group字段（必需）
ALTER TABLE models ADD COLUMN IF NOT EXISTS group_name VARCHAR(100);

-- 添加ownedBy字段（可选）
ALTER TABLE models ADD COLUMN IF NOT EXISTS owned_by VARCHAR(200);

-- 添加description字段（可选）
ALTER TABLE models ADD COLUMN IF NOT EXISTS description TEXT;

-- 添加capabilities字段（可选，使用JSON数组存储）
ALTER TABLE models ADD COLUMN IF NOT EXISTS capabilities TEXT;

-- 添加pricing相关字段（可选）
ALTER TABLE models ADD COLUMN IF NOT EXISTS input_per_million_tokens DECIMAL(10, 6);
ALTER TABLE models ADD COLUMN IF NOT EXISTS output_per_million_tokens DECIMAL(10, 6);
ALTER TABLE models ADD COLUMN IF NOT EXISTS currency_symbol VARCHAR(10) DEFAULT '$';
ALTER TABLE models ADD COLUMN IF NOT EXISTS cached_input_price DECIMAL(10, 6);
ALTER TABLE models ADD COLUMN IF NOT EXISTS cached_output_price DECIMAL(10, 6);

-- 添加supported_text_delta字段（可选）
ALTER TABLE models ADD COLUMN IF NOT EXISTS supported_text_delta BOOLEAN DEFAULT FALSE;

-- 重命名active字段为enabled（如果尚未重命名）
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'models' AND column_name = 'active') AND
       NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'models' AND column_name = 'enabled') THEN
        ALTER TABLE models RENAME COLUMN active TO enabled;
    END IF;
END $$;

-- 创建新字段的索引
CREATE INDEX IF NOT EXISTS idx_models_group ON models(group_name);
CREATE INDEX IF NOT EXISTS idx_models_owned_by ON models(owned_by);

-- 更新表的注释
COMMENT ON TABLE models IS 'AI模型配置表';

-- 添加新字段的注释
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'models' AND column_name = 'group_name') THEN
        COMMENT ON COLUMN models.group_name IS '模型分组';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'models' AND column_name = 'owned_by') THEN
        COMMENT ON COLUMN models.owned_by IS '模型所有者';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'models' AND column_name = 'description') THEN
        COMMENT ON COLUMN models.description IS '模型描述';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'models' AND column_name = 'capabilities') THEN
        COMMENT ON COLUMN models.capabilities IS '模型能力列表（JSON格式）';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'models' AND column_name = 'input_per_million_tokens') THEN
        COMMENT ON COLUMN models.input_per_million_tokens IS '每百万输入token价格';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'models' AND column_name = 'output_per_million_tokens') THEN
        COMMENT ON COLUMN models.output_per_million_tokens IS '每百万输出token价格';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'models' AND column_name = 'currency_symbol') THEN
        COMMENT ON COLUMN models.currency_symbol IS '货币符号';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'models' AND column_name = 'cached_input_price') THEN
        COMMENT ON COLUMN models.cached_input_price IS '缓存输入价格';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'models' AND column_name = 'cached_output_price') THEN
        COMMENT ON COLUMN models.cached_output_price IS '缓存输出价格';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'models' AND column_name = 'supported_text_delta') THEN
        COMMENT ON COLUMN models.supported_text_delta IS '是否支持文本增量输出';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'models' AND column_name = 'enabled') THEN
        COMMENT ON COLUMN models.enabled IS '是否启用';
    END IF;
END $$;
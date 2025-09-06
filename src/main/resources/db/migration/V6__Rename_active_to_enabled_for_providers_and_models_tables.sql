-- V6__Rename_active_to_enabled_for_providers_and_models_tables.sql
-- 重命名providers和models表中的active字段为enabled

-- 检查并重命名providers表的active字段为enabled
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'providers' AND column_name = 'active') THEN
        ALTER TABLE providers RENAME COLUMN active TO enabled;
        RAISE NOTICE 'Renamed providers.active to providers.enabled';
    ELSIF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'providers' AND column_name = 'enabled') THEN
        ALTER TABLE providers ADD COLUMN enabled BOOLEAN DEFAULT TRUE;
        RAISE NOTICE 'Added providers.enabled column';
    END IF;
END $$;

-- 检查并重命名models表的active字段为enabled
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'models' AND column_name = 'active') THEN
        ALTER TABLE models RENAME COLUMN active TO enabled;
        RAISE NOTICE 'Renamed models.active to models.enabled';
    ELSIF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'models' AND column_name = 'enabled') THEN
        ALTER TABLE models ADD COLUMN enabled BOOLEAN DEFAULT TRUE;
        RAISE NOTICE 'Added models.enabled column';
    END IF;
END $$;

-- 创建或更新索引
CREATE INDEX IF NOT EXISTS idx_providers_enabled ON providers(enabled);
CREATE INDEX IF NOT EXISTS idx_models_enabled ON models(enabled);

-- 更新现有记录，将enabled设置为TRUE（如果是新添加的列）
UPDATE providers SET enabled = TRUE WHERE enabled IS NULL;
UPDATE models SET enabled = TRUE WHERE enabled IS NULL;
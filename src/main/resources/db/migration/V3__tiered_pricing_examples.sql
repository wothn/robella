-- 阶梯计费示例数据
-- 这个脚本展示了如何为GPT-4模型配置阶梯计费

-- 1. 首先更新vendor_model的计费策略为TIERED
UPDATE vendor_model 
SET pricing_strategy = 'TIERED'
WHERE vendor_model_name LIKE '%gpt-4%';

-- 2. 为GPT-4模型创建阶梯价格配置
-- 假设vendor_model中GPT-4的ID为1，请根据实际情况修改

-- 第一阶梯：0-100K tokens（基础价格）
INSERT INTO pricing_tier (vendor_model_id, tier_number, min_tokens, max_tokens, 
                         input_per_million_tokens, output_per_million_tokens, 
                         cached_input_price, currency)
VALUES (1, 1, 0, 100000, 30.00, 60.00, 15.00, 'USD');

-- 第二阶梯：100K-1M tokens（中等折扣）
INSERT INTO pricing_tier (vendor_model_id, tier_number, min_tokens, max_tokens, 
                         input_per_million_tokens, output_per_million_tokens, 
                         cached_input_price, currency)
VALUES (1, 2, 100001, 1000000, 25.00, 50.00, 12.50, 'USD');

-- 第三阶梯：1M+ tokens（大用户折扣）
INSERT INTO pricing_tier (vendor_model_id, tier_number, min_tokens, max_tokens, 
                         input_per_million_tokens, output_per_million_tokens, 
                         cached_input_price, currency)
VALUES (1, 3, 1000001, NULL, 20.00, 40.00, 10.00, 'USD');

-- 3. 为Claude模型创建不同的阶梯价格配置（示例）
UPDATE vendor_model 
SET pricing_strategy = 'TIERED'
WHERE vendor_model_name LIKE '%claude-3%';

-- Claude模型的阶梯价格（假设ID为2）
INSERT INTO pricing_tier (vendor_model_id, tier_number, min_tokens, max_tokens, 
                         input_per_million_tokens, output_per_million_tokens, 
                         cached_input_price, currency)
VALUES (2, 1, 0, 50000, 15.00, 75.00, 7.50, 'USD');

INSERT INTO pricing_tier (vendor_model_id, tier_number, min_tokens, max_tokens, 
                         input_per_million_tokens, output_per_million_tokens, 
                         cached_input_price, currency)
VALUES (2, 2, 50001, 500000, 12.00, 60.00, 6.00, 'USD');

INSERT INTO pricing_tier (vendor_model_id, tier_number, min_tokens, max_tokens, 
                         input_per_million_tokens, output_per_million_tokens, 
                         cached_input_price, currency)
VALUES (2, 3, 500001, NULL, 10.00, 50.00, 5.00, 'USD');

-- 4. 查询验证阶梯价格配置
SELECT 
    vm.vendor_model_name,
    vm.pricing_strategy,
    pt.tier_number,
    pt.min_tokens,
    COALESCE(pt.max_tokens::TEXT, '∞') as max_tokens,
    pt.input_per_million_tokens,
    pt.output_per_million_tokens,
    pt.cached_input_price,
    pt.currency
FROM vendor_model vm
JOIN pricing_tier pt ON vm.id = pt.vendor_model_id
WHERE vm.pricing_strategy = 'TIERED'
ORDER BY vm.vendor_model_name, pt.tier_number;
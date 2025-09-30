package org.elmo.robella.service.pricing;

import org.elmo.robella.model.entity.PricingTier;
import org.elmo.robella.model.entity.VendorModel;
import org.elmo.robella.model.enums.PricingStrategyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TieredPricingStrategyTest {
    
    private TieredPricingStrategy tieredPricingStrategy;
    private VendorModel vendorModel;
    private List<PricingTier> pricingTiers;
    
    @BeforeEach
    void setUp() {
        vendorModel = new VendorModel();
        vendorModel.setId(1L);
        vendorModel.setPricingStrategy(PricingStrategyType.TIERED);
        
        // 创建测试阶梯：
        // Tier 1: 0-1000 tokens, $0.01/1K tokens
        // Tier 2: 1001-10000 tokens, $0.008/1K tokens  
        // Tier 3: 10001+ tokens, $0.006/1K tokens
        pricingTiers = Arrays.asList(
            createPricingTier(1, 0, 1000, 0.01, 0.03, 0.005),
            createPricingTier(2, 1001, 10000, 0.008, 0.024, 0.004),
            createPricingTier(3, 10001, Long.MAX_VALUE, 0.006, 0.018, 0.003)
        );
        
        tieredPricingStrategy = new TieredPricingStrategy(pricingTiers);
    }
    
    private PricingTier createPricingTier(int tierNumber, long minTokens, long maxTokens, 
                                          double inputPrice, double outputPrice, double cachedPrice) {
        PricingTier tier = new PricingTier();
        tier.setVendorModelId(1L);
        tier.setTierNumber(tierNumber);
        tier.setMinTokens(minTokens);
        tier.setMaxTokens(maxTokens);
        tier.setInputPerMillionTokens(BigDecimal.valueOf(inputPrice));
        tier.setOutputPerMillionTokens(BigDecimal.valueOf(outputPrice));
        tier.setCachedInputPrice(BigDecimal.valueOf(cachedPrice));
        tier.setCurrency("USD");
        return tier;
    }
    
    @Test
    void testCalculateInputCost_SingleTier() {
        // 测试第一阶梯：500 tokens
        BigDecimal cost = tieredPricingStrategy.calculateInputCost(500, 0);
        // 500 tokens * $0.01/1,000,000 tokens = $0.000005
        assertEquals(new BigDecimal("0.000005"), cost);
    }
    
    @Test
    void testCalculateInputCost_CrossMultipleTiers() {
        // 测试跨阶梯：1500 tokens (全部按第二阶梯价格计算)
        // 1500 tokens * $0.008/1,000,000 = $0.000012
        BigDecimal cost = tieredPricingStrategy.calculateInputCost(1500, 0);
        assertEquals(new BigDecimal("0.000012"), cost);
    }
    
    @Test
    void testCalculateInputCost_WithCaching() {
        // 测试缓存：1500 tokens, 500 cached
        // 固定阶梯定价：所有1500 tokens都按第二阶梯价格计算
        // 缓存部分：500 tokens * $0.004/1,000,000 = $0.000002
        // 非缓存部分：1000 tokens * $0.008/1,000,000 = $0.000008
        // 总成本：$0.000002 + $0.000008 = $0.000010
        BigDecimal cost = tieredPricingStrategy.calculateInputCost(1500, 500);
        assertEquals(new BigDecimal("0.000010"), cost);
    }
    
    @Test
    void testCalculateInputCost_LargeVolume() {
        // 测试大容量：15000 tokens (全部按第三阶梯价格计算)
        // 15000 tokens * $0.006/1,000,000 = $0.00009
        BigDecimal cost = tieredPricingStrategy.calculateInputCost(15000, 0);
        assertEquals(new BigDecimal("0.000090"), cost);
    }
    
    @Test
    void testCalculateOutputCost() {
        // 测试输出成本：2000 tokens (全部按第二阶梯价格计算)
        // 2000 tokens * $0.024/1,000,000 = $0.000048
        BigDecimal cost = tieredPricingStrategy.calculateOutputCost(2000);
        assertEquals(new BigDecimal("0.000048"), cost);
    }
    
    @Test
    void testCalculateTotalCost() {
        // 测试总成本：输入1000 tokens，输出500 tokens
        BigDecimal inputCost = tieredPricingStrategy.calculateInputCost(1000, 0);
        BigDecimal outputCost = tieredPricingStrategy.calculateOutputCost(500);
        BigDecimal totalCost = tieredPricingStrategy.calculateTotalCost(1000, 0, 500);
        
        assertEquals(inputCost.add(outputCost), totalCost);
    }
    
    @Test
    void testGetCurrency() {
        assertEquals("USD", tieredPricingStrategy.getCurrency());
    }
    
    @Test
    void testEdgeCases_ZeroTokens() {
        assertEquals(new BigDecimal("0.000000"), tieredPricingStrategy.calculateInputCost(0, 0));
        assertEquals(new BigDecimal("0.000000"), tieredPricingStrategy.calculateOutputCost(0));
    }
    
    @Test
    void testEdgeCases_ExactTierBoundary() {
        // 测试精确的阶梯边界：1000 tokens
        BigDecimal cost = tieredPricingStrategy.calculateInputCost(1000, 0);
        assertEquals(new BigDecimal("0.000010"), cost);
    }
    
    @Test
    void testInvalidPricingTiers_NullList() {
        assertThrows(IllegalArgumentException.class, () -> {
            new TieredPricingStrategy(null);
        });
    }
    
    @Test
    void testInvalidPricingTiers_EmptyList() {
        assertThrows(IllegalArgumentException.class, () -> {
            new TieredPricingStrategy(Arrays.asList());
        });
    }
}
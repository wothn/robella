package org.elmo.robella.service.pricing;

import org.elmo.robella.model.entity.PricingTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PricingValidationServiceTest {
    
    private PricingValidationService validationService;
    
    @BeforeEach
    void setUp() {
        validationService = new PricingValidationService();
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
    void testValidatePricingTiers_Success() {
        List<PricingTier> tiers = Arrays.asList(
            createPricingTier(1, 0, 1000, 0.01, 0.03, 0.005),
            createPricingTier(2, 1001, 10000, 0.008, 0.024, 0.004),
            createPricingTier(3, 10001, Long.MAX_VALUE, 0.006, 0.018, 0.003)
        );
        
        assertDoesNotThrow(() -> validationService.validatePricingTiers(tiers));
    }
    
    @Test
    void testValidatePricingTiers_EmptyList() {
        List<PricingTier> tiers = Arrays.asList();
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validatePricingTiers(tiers)
        );
        
        assertEquals("Pricing tiers cannot be empty", exception.getMessage());
    }
    
    @Test
    void testValidatePricingTiers_NullList() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validatePricingTiers(null)
        );
        
        assertEquals("Pricing tiers cannot be empty", exception.getMessage());
    }
    
    @Test
    void testValidatePricingTiers_NegativeTierNumber() {
        List<PricingTier> tiers = Arrays.asList(
            createPricingTier(-1, 0, 1000, 0.01, 0.03, 0.005)
        );
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validatePricingTiers(tiers)
        );
        
        assertEquals("Tier number must be positive", exception.getMessage());
    }
    
    @Test
    void testValidatePricingTiers_NegativeMinTokens() {
        List<PricingTier> tiers = Arrays.asList(
            createPricingTier(1, -1, 1000, 0.01, 0.03, 0.005)
        );
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validatePricingTiers(tiers)
        );
        
        assertEquals("Min tokens cannot be negative", exception.getMessage());
    }
    
    @Test
    void testValidatePricingTiers_InvalidTokenRange() {
        List<PricingTier> tiers = Arrays.asList(
            createPricingTier(1, 1000, 500, 0.01, 0.03, 0.005) // max < min
        );
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validatePricingTiers(tiers)
        );
        
        assertEquals("Max tokens must be greater than min tokens", exception.getMessage());
    }
    
    @Test
    void testValidatePricingTiers_NegativePrices() {
        List<PricingTier> tiers = Arrays.asList(
            createPricingTier(1, 0, 1000, -0.01, 0.03, 0.005)
        );
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validatePricingTiers(tiers)
        );
        
        assertEquals("Input price cannot be negative", exception.getMessage());
    }
    
    @Test
    void testValidatePricingTiers_DiscontinuousTiers() {
        List<PricingTier> tiers = Arrays.asList(
            createPricingTier(1, 0, 1000, 0.01, 0.03, 0.005),
            createPricingTier(2, 1002, 10000, 0.008, 0.024, 0.004) // gap at 1001
        );
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validatePricingTiers(tiers)
        );
        
        assertTrue(exception.getMessage().contains("must be exactly one more than previous tier max tokens"));
    }
    
    @Test
    void testValidatePricingTiers_FirstTierNotFromZero() {
        List<PricingTier> tiers = Arrays.asList(
            createPricingTier(1, 100, 1000, 0.01, 0.03, 0.005)
        );
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validatePricingTiers(tiers)
        );
        
        assertEquals("First tier must start from 0 tokens", exception.getMessage());
    }
    
    @Test
    void testValidateTokenCounts_Success() {
        assertDoesNotThrow(() -> validationService.validateTokenCounts(1000, 100, 500));
    }
    
    @Test
    void testValidateTokenCounts_NegativeInput() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validateTokenCounts(-1, 100, 500)
        );
        
        assertEquals("Input tokens cannot be negative", exception.getMessage());
    }
    
    @Test
    void testValidateTokenCounts_CachedExceedsInput() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validateTokenCounts(100, 200, 500)
        );
        
        assertEquals("Cached tokens cannot exceed input tokens", exception.getMessage());
    }
    
    @Test
    void testValidateTokenCounts_ExcessiveTokens() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validateTokenCounts(1_000_000_000_000L, 0, 1_000_000_000_000L)
        );
        
        assertEquals("Total tokens exceed reasonable limit", exception.getMessage());
    }
    
    @Test
    void testCheckTierCoverage_GapAtBeginning() {
        List<PricingTier> tiers = Arrays.asList(
            createPricingTier(1, 100, 1000, 0.01, 0.03, 0.005)
        );
        
        // 应该记录警告但不会抛出异常
        assertDoesNotThrow(() -> validationService.checkTierCoverage(tiers));
    }
    
    @Test
    void testCheckTierCoverage_FiniteLastTier() {
        List<PricingTier> tiers = Arrays.asList(
            createPricingTier(1, 0, 1000, 0.01, 0.03, 0.005),
            createPricingTier(2, 1001, 10000, 0.008, 0.024, 0.004)
        );
        
        // 应该记录警告但不会抛出异常
        assertDoesNotThrow(() -> validationService.checkTierCoverage(tiers));
    }
}
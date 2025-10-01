package org.elmo.robella.service.loadblancer;

import org.elmo.robella.model.entity.PricingTier;
import org.elmo.robella.model.entity.VendorModel;
import org.elmo.robella.model.enums.PricingStrategyType;
import org.elmo.robella.service.PricingTierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class HybridWeightedLoadBalancerTest {

    private PricingTierService pricingTierService;
    private HybridWeightedLoadBalancer loadBalancer;

    @BeforeEach
    void setUp() {
        pricingTierService = mock(PricingTierService.class);
        loadBalancer = new HybridWeightedLoadBalancer(pricingTierService);
    }

    @Test
    void calculateCostWeightShouldFallbackToUnityWhenNoCostData() {
        VendorModel vendorModel = createVendorModel(null, null, PricingStrategyType.FIXED, BigDecimal.ONE);

        BigDecimal result = invokeCostWeight(vendorModel);

        assertThat(result).isEqualByComparingTo(BigDecimal.ONE);
        verifyNoInteractions(pricingTierService);
    }

    @Test
    void calculateCostWeightShouldUseDirectPrices() {
        VendorModel vendorModel = createVendorModel(BigDecimal.valueOf(2), BigDecimal.valueOf(4), PricingStrategyType.FIXED, BigDecimal.ONE);

        BigDecimal result = invokeCostWeight(vendorModel);

        BigDecimal expectedAverage = BigDecimal.valueOf(3).setScale(10);
        assertThat(result).isEqualByComparingTo(BigDecimal.ONE.divide(expectedAverage, 10, java.math.RoundingMode.HALF_UP));
    }

    @Test
    void calculateCostWeightShouldHandleSingleNonNullPrice() {
        VendorModel vendorModel = createVendorModel(BigDecimal.valueOf(2), null, PricingStrategyType.FIXED, BigDecimal.ONE);

        BigDecimal result = invokeCostWeight(vendorModel);

        assertThat(result).isEqualByComparingTo(BigDecimal.ONE.divide(BigDecimal.valueOf(2), 10, java.math.RoundingMode.HALF_UP));
    }

    @Test
    void calculateCostWeightShouldUsePerRequestPrice() {
        VendorModel vendorModel = createVendorModel(null, null, PricingStrategyType.PER_REQUEST, BigDecimal.ONE);
        vendorModel.setPerRequestPrice(BigDecimal.valueOf(3));

        BigDecimal result = invokeCostWeight(vendorModel);

        assertThat(result).isEqualByComparingTo(BigDecimal.ONE.divide(BigDecimal.valueOf(3), 10, java.math.RoundingMode.HALF_UP));
    }

    @Test
    void calculateCostWeightShouldUseTieredPricingWhenModelLevelNull() {
        VendorModel vendorModel = createVendorModel(null, null, PricingStrategyType.TIERED, BigDecimal.ONE);
        vendorModel.setId(42L);
        PricingTier tier = createPricingTier(1, BigDecimal.valueOf(0.5), BigDecimal.valueOf(1.5));
        when(pricingTierService.getPricingTiersByVendorModel(42L)).thenReturn(Collections.singletonList(tier));

        BigDecimal result = invokeCostWeight(vendorModel);

        BigDecimal expectedAverage = BigDecimal.valueOf(1.0).setScale(10);
        assertThat(result).isEqualByComparingTo(BigDecimal.ONE.divide(expectedAverage, 10, java.math.RoundingMode.HALF_UP));
    }

    @Test
    void calculateCostWeightShouldFallbackWhenNoTiersFound() {
        VendorModel vendorModel = createVendorModel(null, null, PricingStrategyType.TIERED, BigDecimal.ONE);
        vendorModel.setId(43L);
        when(pricingTierService.getPricingTiersByVendorModel(43L)).thenReturn(Collections.emptyList());

        BigDecimal result = invokeCostWeight(vendorModel);

        assertThat(result).isEqualByComparingTo(BigDecimal.ONE);
    }

    private BigDecimal invokeCostWeight(VendorModel vendorModel) {
        return (BigDecimal) ReflectionTestUtils.invokeMethod(loadBalancer, "calculateCostWeight", vendorModel);
    }

    private VendorModel createVendorModel(BigDecimal inputPrice, BigDecimal outputPrice, PricingStrategyType strategy, BigDecimal weight) {
        VendorModel vendorModel = new VendorModel();
        vendorModel.setInputPerMillionTokens(inputPrice);
        vendorModel.setOutputPerMillionTokens(outputPrice);
        vendorModel.setPricingStrategy(strategy);
        vendorModel.setWeight(weight);
        vendorModel.setVendorModelName("test model");
        return vendorModel;
    }

    private PricingTier createPricingTier(int tierNumber, BigDecimal inputPrice, BigDecimal outputPrice) {
        PricingTier tier = new PricingTier();
        tier.setTierNumber(tierNumber);
        tier.setInputPerMillionTokens(inputPrice);
        tier.setOutputPerMillionTokens(outputPrice);
        tier.setCachedInputPrice(inputPrice);
        tier.setMinTokens(0L);
        tier.setMaxTokens(null);
        return tier;
    }
}

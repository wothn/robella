package org.elmo.robella.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.mapper.VendorModelMapper;
import org.elmo.robella.model.entity.PricingTier;
import org.elmo.robella.model.entity.VendorModel;
import org.elmo.robella.service.PricingTierService;
import org.elmo.robella.service.pricing.PricingStrategyFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/pricing")
@RequiredArgsConstructor
public class PricingController {
    
    private final PricingTierService pricingTierService;
    private final VendorModelMapper vendorModelMapper;
    private final PricingStrategyFactory pricingStrategyFactory;
    
    /**
     * 获取供应商模型的定价阶梯
     * @param vendorModelId 供应商模型ID
     * @return 定价阶梯列表
     */
    @GetMapping("/tiers/{vendorModelId}")
    public ResponseEntity<List<PricingTier>> getPricingTiers(@PathVariable Long vendorModelId) {
        try {
            List<PricingTier> tiers = pricingTierService.getPricingTiersByVendorModel(vendorModelId);
            return ResponseEntity.ok(tiers);
        } catch (Exception e) {
            log.error("Failed to get pricing tiers for vendor model {}: {}", vendorModelId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 创建或更新定价阶梯
     * @param vendorModelId 供应商模型ID
     * @param pricingTiers 定价阶梯列表
     * @return 操作结果
     */
    @PostMapping("/tiers/{vendorModelId}")
    public ResponseEntity<Void> createPricingTiers(
            @PathVariable Long vendorModelId,
            @RequestBody List<PricingTier> pricingTiers) {
        try {
            // 验证定价阶梯的完整性
            pricingTierService.validatePricingTiers(pricingTiers);
            
            // 创建定价阶梯
            pricingTierService.createPricingTiers(vendorModelId, pricingTiers);
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to create pricing tiers for vendor model {}: {}", vendorModelId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 更新定价阶梯
     * @param pricingTierId 定价阶梯ID
     * @param updatedTier 更新的阶梯信息
     * @return 操作结果
     */
    @PutMapping("/tiers/{vendorModelId}")
    public ResponseEntity<Void> updatePricingTiers(
            @PathVariable Long vendorModelId,
            @RequestBody List<PricingTier> pricingTiers) {
        try {
            // 验证定价阶梯的完整性
            pricingTierService.validatePricingTiers(pricingTiers);
            
            // 更新定价阶梯
            pricingTierService.createPricingTiers(vendorModelId, pricingTiers);
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to update pricing tiers for vendor model {}: {}", vendorModelId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 删除定价阶梯
     * @param pricingTierId 定价阶梯ID
     * @return 操作结果
     */
    @DeleteMapping("/tiers/{pricingTierId}")
    public ResponseEntity<Void> deletePricingTier(@PathVariable Long pricingTierId) {
        try {
            pricingTierService.deletePricingTier(pricingTierId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to delete pricing tier {}: {}", pricingTierId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 计算示例成本
     * @param vendorModelId 供应商模型ID
     * @param inputTokens 输入令牌数
     * @param cachedTokens 缓存令牌数
     * @param outputTokens 输出令牌数
     * @return 成本计算结果
     */
    @GetMapping("/calculate/{vendorModelId}")
    public ResponseEntity<BigDecimal> calculateCost(
            @PathVariable Long vendorModelId,
            @RequestParam long inputTokens,
            @RequestParam(defaultValue = "0") long cachedTokens,
            @RequestParam long outputTokens) {
        try {
            VendorModel vendorModel = vendorModelMapper.selectById(vendorModelId);
            if (vendorModel == null) {
                return ResponseEntity.badRequest().build();
            }
            
            // 使用计费策略工厂计算成本
            org.elmo.robella.service.pricing.PricingStrategy strategy = 
                pricingStrategyFactory.createPricingStrategy(vendorModel);
            
            BigDecimal inputCost = strategy.calculateInputCost(inputTokens, cachedTokens);
            BigDecimal outputCost = strategy.calculateOutputCost(outputTokens);
            BigDecimal totalCost = inputCost.add(outputCost);
            
            return ResponseEntity.ok(totalCost);
        } catch (Exception e) {
            log.error("Failed to calculate cost for vendor model {}: {}", vendorModelId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 成本计算响应
     */
    public record CostCalculationResponse(
        long inputTokens,
        long cachedTokens,
        long outputTokens,
        BigDecimal inputCost,
        BigDecimal outputCost,
        BigDecimal totalCost,
        String currency,
        String pricingStrategy
    ) {}
}
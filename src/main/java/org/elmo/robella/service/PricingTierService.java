package org.elmo.robella.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.mapper.PricingTierMapper;
import org.elmo.robella.mapper.VendorModelMapper;
import org.elmo.robella.model.entity.PricingTier;
import org.elmo.robella.model.entity.VendorModel;
import org.elmo.robella.model.enums.PricingStrategyType;
import org.elmo.robella.service.pricing.PricingValidationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PricingTierService extends ServiceImpl<PricingTierMapper, PricingTier> {
    
    private final PricingTierMapper pricingTierMapper;
    private final VendorModelMapper vendorModelMapper;
    private final PricingValidationService validationService;
    
    /**
     * 获取供应商模型的所有定价阶梯
     * @param vendorModelId 供应商模型ID
     * @return 定价阶梯列表
     */
    public List<PricingTier> getPricingTiersByVendorModel(Long vendorModelId) {
        return pricingTierMapper.findByVendorModelId(vendorModelId);
    }
    
    /**
     * 为供应商模型创建定价阶梯
     * @param vendorModelId 供应商模型ID
     * @param pricingTiers 定价阶梯列表
     */
    @Transactional
    public void createPricingTiers(Long vendorModelId, List<PricingTier> pricingTiers) {
        // 验证供应商模型存在
        VendorModel vendorModel = vendorModelMapper.selectById(vendorModelId);
        if (vendorModel == null) {
            throw new IllegalArgumentException("Vendor model not found: " + vendorModelId);
        }
        
        // 删除现有的定价阶梯
        pricingTierMapper.deleteByVendorModelId(vendorModelId);
        
        // 设置供应商模型ID并创建新的定价阶梯
        for (PricingTier tier : pricingTiers) {
            tier.setVendorModelId(vendorModelId);
            pricingTierMapper.insert(tier);
        }
        
        // 更新供应商模型的计费策略为阶梯计费
        vendorModel.setPricingStrategy(PricingStrategyType.TIERED);
        vendorModelMapper.updateById(vendorModel);
        
        log.info("Created {} pricing tiers for vendor model {}", pricingTiers.size(), vendorModelId);
    }
    
    /**
     * 更新定价阶梯
     * @param pricingTierId 定价阶梯ID
     * @param updatedTier 更新的阶梯信息
     */
    @Transactional
    public void updatePricingTier(Long pricingTierId, PricingTier updatedTier) {
        PricingTier existingTier = pricingTierMapper.selectById(pricingTierId);
        if (existingTier == null) {
            throw new IllegalArgumentException("Pricing tier not found: " + pricingTierId);
        }
        
        // 更新字段
        existingTier.setTierNumber(updatedTier.getTierNumber());
        existingTier.setMinTokens(updatedTier.getMinTokens());
        existingTier.setMaxTokens(updatedTier.getMaxTokens());
        existingTier.setInputPerMillionTokens(updatedTier.getInputPerMillionTokens());
        existingTier.setOutputPerMillionTokens(updatedTier.getOutputPerMillionTokens());
        existingTier.setCachedInputPrice(updatedTier.getCachedInputPrice());
        
        pricingTierMapper.updateById(existingTier);
        log.info("Updated pricing tier {}", pricingTierId);
    }
    
    /**
     * 删除定价阶梯
     * @param pricingTierId 定价阶梯ID
     */
    @Transactional
    public void deletePricingTier(Long pricingTierId) {
        PricingTier tier = pricingTierMapper.selectById(pricingTierId);
        if (tier == null) {
            throw new IllegalArgumentException("Pricing tier not found: " + pricingTierId);
        }
        
        pricingTierMapper.deleteById(pricingTierId);
        
        // 检查是否还有其他阶梯，如果没有则恢复为固定价格
        Long vendorModelId = tier.getVendorModelId();
        List<PricingTier> remainingTiers = pricingTierMapper.findByVendorModelId(vendorModelId);
        if (remainingTiers.isEmpty()) {
            VendorModel vendorModel = vendorModelMapper.selectById(vendorModelId);
            if (vendorModel != null) {
                vendorModel.setPricingStrategy(PricingStrategyType.FIXED);
                vendorModelMapper.updateById(vendorModel);
                log.info("No more pricing tiers for vendor model {}, switched back to FIXED pricing", vendorModelId);
            }
        }
        
        log.info("Deleted pricing tier {}", pricingTierId);
    }
    
    /**
     * 批量保存定价阶梯
     * @param pricingTiers 定价阶梯列表
     * @return 是否成功
     */
    @Transactional
    public boolean saveBatch(List<PricingTier> pricingTiers) {
        if (pricingTiers == null || pricingTiers.isEmpty()) {
            return true;
        }

        try {
            boolean result = this.saveBatch(pricingTiers, 1000); // 使用MyBatis-Plus原生批量插入，每批1000条
            log.info("Saved {} pricing tiers in batch, result: {}", pricingTiers.size(), result);
            return result;
        } catch (Exception e) {
            log.error("Failed to save pricing tiers in batch", e);
            return false;
        }
    }

    /**
     * 根据供应商模型ID删除所有定价阶梯
     * @param vendorModelId 供应商模型ID
     * @return 是否成功
     */
    @Transactional
    public boolean deleteByVendorModelId(Long vendorModelId) {
        try {
            pricingTierMapper.deleteByVendorModelId(vendorModelId);
            log.info("Deleted all pricing tiers for vendor model {}", vendorModelId);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete pricing tiers for vendor model {}", vendorModelId, e);
            return false;
        }
    }

    /**
     * 验证定价阶梯的完整性
     * @param pricingTiers 定价阶梯列表
     */
    public void validatePricingTiers(List<PricingTier> pricingTiers) {
        validationService.validatePricingTiers(pricingTiers);
        validationService.checkTierCoverage(pricingTiers);
    }
}
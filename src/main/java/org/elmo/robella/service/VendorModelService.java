package org.elmo.robella.service;

import org.elmo.robella.mapper.VendorModelMapper;
import org.elmo.robella.mapper.PricingTierMapper;
import org.elmo.robella.mapper.ModelMapper;
import org.elmo.robella.mapper.ProviderMapper;
import org.elmo.robella.model.entity.VendorModel;
import org.elmo.robella.model.entity.PricingTier;
import org.elmo.robella.model.entity.Model;
import org.elmo.robella.model.entity.Provider;
import org.elmo.robella.model.dto.VendorModelDTO;
import org.elmo.robella.model.enums.PricingStrategyType;
import org.elmo.robella.exception.ValidationException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorModelService extends ServiceImpl<VendorModelMapper, VendorModel> {

    private final PricingTierMapper pricingTierMapper;
    private final ModelMapper modelMapper;
    private final ProviderMapper providerMapper;

    /**
     * 获取所有VendorModel
     */
    public List<VendorModelDTO> getAllVendorModels() {
        List<VendorModel> vendorModels = this.list();
        return vendorModels.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取VendorModel
     */
    public VendorModelDTO getVendorModelById(Long id) {
        VendorModel vendorModel = this.getById(id);
        if (vendorModel == null) {
            return null;
        }
        return convertToDTO(vendorModel);
    }

    /**
     * 根据Model ID获取VendorModels
     */
    public List<VendorModelDTO> getVendorModelsByModelId(Long modelId) {
        List<VendorModel> vendorModels = baseMapper.findByModelId(modelId);
        return vendorModels.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据Provider ID获取VendorModels
     */
    public List<VendorModelDTO> getVendorModelsByProviderId(Long providerId) {
        List<VendorModel> vendorModels = baseMapper.findByProviderId(providerId);
        return vendorModels.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取启用的VendorModels
     */
    public List<VendorModelDTO> getEnabledVendorModels() {
        List<VendorModel> vendorModels = baseMapper.findByEnabledTrue();
        return vendorModels.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据模型Key获取启用的VendorModels
     */
    public List<VendorModelDTO> getEnabledVendorModelsByModelKey(String modelKey) {
        List<VendorModel> vendorModels = baseMapper.findByModelKeyAndEnabledTrue(modelKey);
        return vendorModels.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 创建VendorModel
     */
    @Transactional
    public VendorModelDTO createVendorModel(VendorModelDTO.CreateRequest request) {
        validateCreateRequest(request);

        VendorModel vendorModel = request.getVendorModel();

        // 验证关联的Model和Provider是否存在
        validateModelAndProvider(vendorModel.getModelId(), vendorModel.getProviderId());

        // 检查vendorModelKey是否已存在
        validateUniqueVendorModelKey(vendorModel.getVendorModelKey());

        // 设置默认值
        if (vendorModel.getWeight() == null) {
            vendorModel.setWeight(BigDecimal.valueOf(5.0));
        }
        if (vendorModel.getEnabled() == null) {
            vendorModel.setEnabled(true);
        }
        if (vendorModel.getCachedInputPrice() == null) {
            vendorModel.setCachedInputPrice(BigDecimal.ZERO);
        }

        boolean saved = this.save(vendorModel);
        if (!saved) {
            throw new RuntimeException("Failed to save vendor model");
        }

        // 保存阶梯计费配置
        if (request.getPricingTiers() != null && !request.getPricingTiers().isEmpty()) {
            savePricingTiers(vendorModel.getId(), request.getPricingTiers());
        }

        log.info("Created vendor model: {}", vendorModel.getVendorModelName());
        return convertToDTO(vendorModel);
    }

    /**
     * 更新VendorModel
     */
    @Transactional
    public VendorModelDTO updateVendorModel(Long id, VendorModelDTO.UpdateRequest request) {
        VendorModel existingVendorModel = this.getById(id);
        if (existingVendorModel == null) {
            throw new ValidationException("VENDOR_MODEL_NOT_FOUND", "Vendor model not found with id: " + id);
        }

        VendorModel vendorModel = request.getVendorModel();
        vendorModel.setId(id);

        if (!StringUtils.hasText(vendorModel.getVendorModelName())) {
            vendorModel.setVendorModelName(existingVendorModel.getVendorModelName());
        }
        if (!StringUtils.hasText(vendorModel.getVendorModelKey())) {
            vendorModel.setVendorModelKey(existingVendorModel.getVendorModelKey());
        }
        if (!StringUtils.hasText(vendorModel.getModelKey())) {
            vendorModel.setModelKey(existingVendorModel.getModelKey());
        }
        if (vendorModel.getProviderId() == null) {
            vendorModel.setProviderId(existingVendorModel.getProviderId());
        }
        if (vendorModel.getProviderType() == null) {
            vendorModel.setProviderType(existingVendorModel.getProviderType());
        }
        if (vendorModel.getPricingStrategy() == null) {
            vendorModel.setPricingStrategy(existingVendorModel.getPricingStrategy());
        }
        if (vendorModel.getCachedInputPrice() == null) {
            vendorModel.setCachedInputPrice(existingVendorModel.getCachedInputPrice());
        }
        if (vendorModel.getWeight() == null) {
            vendorModel.setWeight(existingVendorModel.getWeight());
        }
        if (vendorModel.getEnabled() == null) {
            vendorModel.setEnabled(existingVendorModel.getEnabled());
        }

        // 验证关联的Model和Provider是否存在
        validateModelAndProvider(vendorModel.getModelId(), vendorModel.getProviderId());

        // 如果vendorModelKey发生变化，检查唯一性
        if (!existingVendorModel.getVendorModelKey().equals(vendorModel.getVendorModelKey())) {
            validateUniqueVendorModelKey(vendorModel.getVendorModelKey());
        }

        // 验证定价信息
        validatePricingInfo(vendorModel);

        boolean updated = this.updateById(vendorModel);
        if (!updated) {
            throw new RuntimeException("Failed to update vendor model");
        }

        // 更新阶梯计费配置
        if (request.getPricingTiers() != null) {
            // 删除原有的阶梯计费配置
            deletePricingTiersByVendorModelId(id);
            // 保存新的阶梯计费配置
            if (!request.getPricingTiers().isEmpty()) {
                savePricingTiers(id, request.getPricingTiers());
            }
        }

        log.info("Updated vendor model: {}", vendorModel.getVendorModelName());
        return convertToDTO(vendorModel);
    }

    /**
     * 删除VendorModel
     */
    @Transactional
    public boolean deleteVendorModel(Long id) {
        VendorModel vendorModel = this.getById(id);
        if (vendorModel == null) {
            return false;
        }

        // 删除关联的阶梯计费配置
        deletePricingTiersByVendorModelId(id);

        boolean deleted = this.removeById(id);
        if (deleted) {
            log.info("Deleted vendor model: {}", vendorModel.getVendorModelName());
        }
        return deleted;
    }

    /**
     * 启用/禁用VendorModel
     */
    public VendorModelDTO toggleVendorModelStatus(Long id, boolean enabled) {
        VendorModel vendorModel = this.getById(id);
        if (vendorModel == null) {
            throw new ValidationException("VENDOR_MODEL_NOT_FOUND", "Vendor model not found with id: " + id);
        }

        vendorModel.setEnabled(enabled);
        boolean updated = this.updateById(vendorModel);
        if (!updated) {
            throw new RuntimeException("Failed to update vendor model status");
        }

        log.info("{} vendor model: {}", enabled ? "Enabled" : "Disabled", vendorModel.getVendorModelName());
        return convertToDTO(vendorModel);
    }

    /**
     * 根据vendorModelKey查找VendorModel
     */
    public VendorModelDTO findByVendorModelKey(String vendorModelKey) {
        VendorModel vendorModel = baseMapper.findByVendorModelKey(vendorModelKey);
        return vendorModel != null ? convertToDTO(vendorModel) : null;
    }

    /**
     * 根据vendorModelKey查找启用的VendorModel
     */
    public VendorModelDTO findEnabledByVendorModelKey(String vendorModelKey) {
        VendorModel vendorModel = baseMapper.findByVendorModelKeyAndEnabledTrue(vendorModelKey);
        return vendorModel != null ? convertToDTO(vendorModel) : null;
    }

    // 私有辅助方法

    private void validateCreateRequest(VendorModelDTO.CreateRequest request) {
        if (request == null || request.getVendorModel() == null) {
            throw new ValidationException("INVALID_VENDOR_MODEL_DATA", "Vendor model data is required");
        }

        VendorModel vendorModel = request.getVendorModel();

        if (!StringUtils.hasText(vendorModel.getVendorModelName())) {
            throw new ValidationException("VENDOR_MODEL_NAME_REQUIRED", "Vendor model name is required");
        }

        if (!StringUtils.hasText(vendorModel.getVendorModelKey())) {
            throw new ValidationException("VENDOR_MODEL_KEY_REQUIRED", "Vendor model key is required");
        }

        if (vendorModel.getProviderId() == null) {
            throw new ValidationException("PROVIDER_ID_REQUIRED", "Provider ID is required");
        }

        if (vendorModel.getPricingStrategy() == null) {
            throw new ValidationException("PRICING_STRATEGY_REQUIRED", "Pricing strategy is required");
        }

        if (!StringUtils.hasText(vendorModel.getModelKey())) {
            vendorModel.setModelKey(vendorModel.getVendorModelKey());
        }

        // 验证定价信息
        validatePricingInfo(vendorModel);
    }

    private void validateModelAndProvider(Long modelId, Long providerId) {
        if (providerId == null) {
            throw new ValidationException("PROVIDER_ID_REQUIRED", "Provider ID is required");
        }

        if (modelId != null) {
            Model model = modelMapper.selectById(modelId);
            if (model == null) {
                throw new ValidationException("MODEL_NOT_FOUND", "Model not found with id: " + modelId);
            }
        }

        Provider provider = providerMapper.selectById(providerId);
        if (provider == null) {
            throw new ValidationException("PROVIDER_NOT_FOUND", "Provider not found with id: " + providerId);
        }
    }

    private void validateUniqueVendorModelKey(String vendorModelKey) {
        VendorModel existing = baseMapper.findByVendorModelKey(vendorModelKey);
        if (existing != null) {
            throw new ValidationException("VENDOR_MODEL_KEY_EXISTS", "Vendor model key already exists: " + vendorModelKey);
        }
    }

    private void validatePricingInfo(VendorModel vendorModel) {
        // 根据定价策略验证相应的价格字段
        switch (vendorModel.getPricingStrategy()) {
            case FIXED:
                if (vendorModel.getCachedInputPrice() == null || vendorModel.getCachedInputPrice().compareTo(BigDecimal.ZERO) < 0) {
                    throw new ValidationException("INVALID_FIXED_PRICING", "Cached input price is required and must be non-negative for fixed pricing");
                }
                break;
            case PER_REQUEST:
                if (vendorModel.getPerRequestPrice() == null || vendorModel.getPerRequestPrice().compareTo(BigDecimal.ZERO) < 0) {
                    throw new ValidationException("INVALID_PER_REQUEST_PRICING", "Per request price is required and must be non-negative for per-request pricing");
                }
                break;
            case TIERED:
                // 阶梯计费的验证在pricingTiers中进行
                break;
        }
    }

    private void savePricingTiers(Long vendorModelId, List<PricingTier> pricingTiers) {
        for (PricingTier tier : pricingTiers) {
            tier.setVendorModelId(vendorModelId);
            tier.setCreatedAt(OffsetDateTime.now());
            pricingTierMapper.insert(tier);
        }
    }

    private void deletePricingTiersByVendorModelId(Long vendorModelId) {
        LambdaQueryWrapper<PricingTier> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PricingTier::getVendorModelId, vendorModelId);
        pricingTierMapper.delete(queryWrapper);
    }

    private VendorModelDTO convertToDTO(VendorModel vendorModel) {
        VendorModelDTO dto = new VendorModelDTO();
        dto.setId(vendorModel.getId());
        dto.setModelId(vendorModel.getModelId());
        dto.setModelKey(vendorModel.getModelKey());
        dto.setProviderId(vendorModel.getProviderId());
        dto.setVendorModelName(vendorModel.getVendorModelName());
        dto.setVendorModelKey(vendorModel.getVendorModelKey());
        dto.setProviderType(vendorModel.getProviderType());
        dto.setDescription(vendorModel.getDescription());
        dto.setInputPerMillionTokens(vendorModel.getInputPerMillionTokens());
        dto.setOutputPerMillionTokens(vendorModel.getOutputPerMillionTokens());
        dto.setPerRequestPrice(vendorModel.getPerRequestPrice());
        dto.setCurrency(vendorModel.getCurrency());
        dto.setCachedInputPrice(vendorModel.getCachedInputPrice());
        dto.setPricingStrategy(vendorModel.getPricingStrategy());
        dto.setWeight(vendorModel.getWeight());
        dto.setEnabled(vendorModel.getEnabled());
        dto.setCreatedAt(vendorModel.getCreatedAt());
        dto.setUpdatedAt(vendorModel.getUpdatedAt());

        // 加载阶梯计费配置
        if (vendorModel.getPricingStrategy() == PricingStrategyType.TIERED) {
            LambdaQueryWrapper<PricingTier> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PricingTier::getVendorModelId, vendorModel.getId())
                       .orderByAsc(PricingTier::getMinTokens);
            List<PricingTier> tiers = pricingTierMapper.selectList(queryWrapper);
            dto.setPricingTiers(tiers);
        }

        return dto;
    }
}
package org.elmo.robella.util;

import org.elmo.robella.model.entity.VendorModel;
import org.elmo.robella.model.dto.VendorModelDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * VendorModel和DTO之间的转换工具类
 */
@Component
public class VendorModelConverter {

    /**
     * 将VendorModel转换为VendorModelDTO
     */
    public VendorModelDTO convertToDTO(VendorModel vendorModel) {
        if (vendorModel == null) {
            return null;
        }

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

        return dto;
    }

    /**
     * 将VendorModelDTO转换为VendorModel
     */
    public VendorModel convertToEntity(VendorModelDTO dto) {
        if (dto == null) {
            return null;
        }

        VendorModel vendorModel = new VendorModel();
        vendorModel.setId(dto.getId());
        vendorModel.setModelId(dto.getModelId());
        vendorModel.setModelKey(dto.getModelKey());
        vendorModel.setProviderId(dto.getProviderId());
        vendorModel.setVendorModelName(dto.getVendorModelName());
        vendorModel.setVendorModelKey(dto.getVendorModelKey());
        vendorModel.setProviderType(dto.getProviderType());
        vendorModel.setDescription(dto.getDescription());
        vendorModel.setInputPerMillionTokens(dto.getInputPerMillionTokens());
        vendorModel.setOutputPerMillionTokens(dto.getOutputPerMillionTokens());
        vendorModel.setPerRequestPrice(dto.getPerRequestPrice());
        vendorModel.setCurrency(dto.getCurrency());
        vendorModel.setCachedInputPrice(dto.getCachedInputPrice());
        vendorModel.setPricingStrategy(dto.getPricingStrategy());
        vendorModel.setWeight(dto.getWeight());
        vendorModel.setEnabled(dto.getEnabled());
        vendorModel.setCreatedAt(dto.getCreatedAt());
        vendorModel.setUpdatedAt(dto.getUpdatedAt());

        return vendorModel;
    }

    /**
     * 将VendorModel列表转换为VendorModelDTO列表
     */
    public List<VendorModelDTO> convertToDTOList(List<VendorModel> vendorModels) {
        if (vendorModels == null) {
            return null;
        }
        return vendorModels.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 将VendorModelDTO列表转换为VendorModel列表
     */
    public List<VendorModel> convertToEntityList(List<VendorModelDTO> dtos) {
        if (dtos == null) {
            return null;
        }
        return dtos.stream()
                .map(this::convertToEntity)
                .collect(Collectors.toList());
    }

    /**
     * 将CreateRequest转换为VendorModel
     */
    public VendorModel convertCreateRequestToEntity(VendorModelDTO.CreateRequest request) {
        if (request == null || request.getVendorModel() == null) {
            return null;
        }
        return request.getVendorModel();
    }

    /**
     * 将UpdateRequest转换为VendorModel
     */
    public VendorModel convertUpdateRequestToEntity(VendorModelDTO.UpdateRequest request) {
        if (request == null || request.getVendorModel() == null) {
            return null;
        }
        return request.getVendorModel();
    }

    /**
     * 合并DTO到Entity（用于更新操作）
     */
    public void mergeDTOToEntity(VendorModelDTO source, VendorModel target) {
        if (source == null || target == null) {
            return;
        }

        target.setModelId(source.getModelId());
        target.setModelKey(source.getModelKey());
        target.setProviderId(source.getProviderId());
        target.setVendorModelName(source.getVendorModelName());
        target.setVendorModelKey(source.getVendorModelKey());
        target.setProviderType(source.getProviderType());
        target.setDescription(source.getDescription());
        target.setInputPerMillionTokens(source.getInputPerMillionTokens());
        target.setOutputPerMillionTokens(source.getOutputPerMillionTokens());
        target.setPerRequestPrice(source.getPerRequestPrice());
        target.setCurrency(source.getCurrency());
        target.setCachedInputPrice(source.getCachedInputPrice());
        target.setPricingStrategy(source.getPricingStrategy());
        target.setWeight(source.getWeight());
        target.setEnabled(source.getEnabled());
    }
}
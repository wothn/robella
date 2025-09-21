package org.elmo.robella.service;

import org.elmo.robella.model.entity.Provider;
import org.elmo.robella.model.entity.VendorModel;
import org.elmo.robella.exception.ResourceNotFoundException;
import org.elmo.robella.mapper.ProviderMapper;
import org.elmo.robella.mapper.VendorModelMapper;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProviderService extends ServiceImpl<ProviderMapper, Provider> {

    private final VendorModelMapper vendorModelMapper;

    // Provider methods
    public List<Provider> getAllProviders() {
        return list();
    }

    public List<Provider> getEnabledProviders() {
        LambdaQueryWrapper<Provider> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(Provider::getEnabled, true);
        return list(queryWrapper);
    }

    public Provider getProviderById(Long id) {
        return getById(id);
    }

    public Provider getProviderByName(String name) {
        return this.getOne(Wrappers.<Provider>lambdaQuery().eq(Provider::getName, name));
    }

    public boolean createProvider(Provider provider) {
        provider.setEnabled(true);
        return save(provider);
    }

    public boolean updateProvider(Long id, Provider provider) {
        Provider existingProvider = getById(id);
        if (existingProvider == null) {
            throw new RuntimeException("Provider not found with id: " + id);
        }

        provider.setId(id);
        return updateById(provider);
    }

    public boolean deleteProvider(Long id) {
        List<VendorModel> vendorModels = vendorModelMapper.findByProviderId(id);
        if (!vendorModels.isEmpty()) {
            throw new RuntimeException("Cannot delete provider with existing vendor models");
        }
        return removeById(id);
    }

    // VendorModel methods
    public List<VendorModel> getVendorModelsByProviderId(Long providerId) {
        return vendorModelMapper.findByProviderId(providerId);
    }

    public boolean createVendorModel(Long providerId, VendorModel vendorModel) {
        vendorModel.setProviderId(providerId);
        vendorModel.setEnabled(true);
        return vendorModelMapper.insert(vendorModel) > 0;
    }

    public boolean updateVendorModel(Long id, VendorModel vendorModel) {
        VendorModel existingVendorModel = vendorModelMapper.selectById(id);
        if (existingVendorModel == null) {
            throw new ResourceNotFoundException("VendorModel not found with id: " + id);
        }

        vendorModel.setId(id);
        return vendorModelMapper.updateById(vendorModel) > 0;
    }

    public boolean deleteVendorModel(Long id) {
        return vendorModelMapper.deleteById(id) > 0;
    }
}
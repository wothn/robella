package org.elmo.robella.service;

import org.elmo.robella.mapper.ModelMapper;
import org.elmo.robella.mapper.VendorModelMapper;
import org.elmo.robella.model.entity.Model;
import org.elmo.robella.model.entity.VendorModel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelService extends ServiceImpl<ModelMapper, Model> {
    
    private final VendorModelMapper vendorModelMapper;
    
    
    public List<Model> findByPublishedTrue() {
        LambdaQueryWrapper<Model> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Model::getPublished, true);
        return this.list(queryWrapper);
    }
    
    public List<Model> searchModels(String keyword) {
        LambdaQueryWrapper<Model> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Model::getName, keyword)
                   .or()
                   .like(Model::getModelKey, keyword)
                   .or()
                   .like(Model::getDescription, keyword);
        return this.list(queryWrapper);
    }
    
    public List<Model> findByCapability(String capability) {
        LambdaQueryWrapper<Model> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Model::getCapabilities, capability);
        return this.list(queryWrapper);
    }
    
    public List<VendorModel> getVendorModelsByModelId(Long modelId) {
        return vendorModelMapper.findByModelId(modelId);
    }
    
    @Transactional
    public VendorModel addVendorModelToModel(Long modelId, Long vendorModelId) {
        // 验证模型是否存在
        Model model = this.getById(modelId);
        if (model == null) {
            return null;
        }
        
        // 获取VendorModel
        VendorModel vendorModel = vendorModelMapper.selectById(vendorModelId);
        if (vendorModel == null) {
            return null;
        }
        
        // 更新VendorModel的modelId
        vendorModel.setModelId(modelId);
        vendorModelMapper.updateById(vendorModel);
        
        return vendorModel;
    }
}

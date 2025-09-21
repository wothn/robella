package org.elmo.robella.service;

import org.elmo.robella.mapper.ModelMapper;
import org.elmo.robella.mapper.VendorModelMapper;
import org.springframework.stereotype.Service;
import org.elmo.robella.model.entity.Model;
import org.elmo.robella.model.entity.VendorModel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ModelService extends ServiceImpl<ModelMapper, Model> {
    public final VendorModelMapper vendorModelMapper;

    public ModelService(ModelMapper modelMapper, VendorModelMapper vendorModelMapper) {
        this.vendorModelMapper = vendorModelMapper;
    }

    public List<Model> getModels() {
        return list();
    }

    public List<Model> getPublishedModels() {
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Model::getPublished, true);
        return list(wrapper);
    }

    public List<Model> getModelsByOrganization(String organization) {
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Model::getOrganization, organization);
        return list(wrapper);
    }

    public int createModel(Model model) {
        return save(model) ? 1 : 0;
    }

    public int updateModel(Model model) {
        if (model.getId() == null) {
            throw new IllegalArgumentException("Model ID cannot be null");
        }

        Model existingModel = getById(model.getId());
        if (existingModel == null) {
            throw new IllegalArgumentException("Model not found");
        }

        // 更新所有字段
        existingModel.setName(model.getName());
        existingModel.setModelKey(model.getModelKey());
        existingModel.setDescription(model.getDescription());
        existingModel.setOrganization(model.getOrganization());
        existingModel.setCapabilities(model.getCapabilities());
        existingModel.setContextWindow(model.getContextWindow());
        existingModel.setPublished(model.getPublished());

        return updateById(existingModel) ? 1 : 0;
    }

    public int deleteModel(Long id) {
        return removeById(id) ? 1 : 0;
    }

    public Model getModelById(Long id) {
        return getById(id);
    }

    public Model getModelByModelKey(String modelKey) {
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Model::getModelKey, modelKey);
        return getOne(wrapper);
    }

    public List<VendorModel> getVendorModelsByModelId(Long modelId) {
        return vendorModelMapper.findByModelId(modelId);
    }

    public List<VendorModel> getVendorModelsByModelKey(String modelKey) {
        return vendorModelMapper.findByModelKeyAndEnabledTrue(modelKey);
    }

    // 新增功能：状态管理
    public int publishModel(Long id) {
        Model model = getById(id);
        if (model == null) {
            throw new IllegalArgumentException("Model not found");
        }
        model.setPublished(true);
        return updateById(model) ? 1 : 0;
    }

    public int unpublishModel(Long id) {
        Model model = getById(id);
        if (model == null) {
            throw new IllegalArgumentException("Model not found");
        }
        model.setPublished(false);
        return updateById(model) ? 1 : 0;
    }

    // 新增功能：搜索和筛选
    public List<Model> searchModelsByName(String keyword) {
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Model::getName, keyword);
        return list(wrapper);
    }

    public List<Model> getModelsByCapability(String capability) {
        List<Model> allModels = list();
        return allModels.stream()
                .filter(model -> model.getCapabilities() != null &&
                        model.getCapabilities().stream()
                                .anyMatch(cap -> cap.name().equalsIgnoreCase(capability)))
                .collect(Collectors.toList());
    }

    // 新增功能：统计信息
    public long countTotalModels() {
        return count();
    }

    public long countPublishedModels() {
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Model::getPublished, true);
        return count(wrapper);
    }

    public long countModelsByOrganization(String organization) {
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Model::getOrganization, organization);
        return count(wrapper);
    }

    // 新增功能：模型验证
    public boolean modelExistsByName(String name) {
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Model::getName, name);
        return count(wrapper) > 0;
    }

    public boolean modelExistsByModelKey(String modelKey) {
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Model::getModelKey, modelKey);
        return count(wrapper) > 0;
    }

    public int validateAndCreateModel(Model model) {
        if (modelExistsByName(model.getName())) {
            throw new IllegalArgumentException("Model name already exists");
        }
        if (modelExistsByModelKey(model.getModelKey())) {
            throw new IllegalArgumentException("Model key already exists");
        }
        return createModel(model);
    }

    // 修正方法名的拼写错误
    public int addVendorModelForModel(Long modelId, Long vendorModelId) {

        VendorModel vendorModel = vendorModelMapper.selectById(vendorModelId);
        String modelKey = this.getModelById(modelId).getModelKey();
        if (vendorModel == null) {
            throw new IllegalArgumentException("VendorModel not found");
        }
        vendorModel.setModelKey(modelKey);
        vendorModel.setModelId(modelId);
        return vendorModelMapper.updateById(vendorModel);
    }
}

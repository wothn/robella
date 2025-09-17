package org.elmo.robella.service;

import org.elmo.robella.repository.ModelRepository;
import org.elmo.robella.repository.VendorModelRepository;
import org.springframework.stereotype.Service;
import org.elmo.robella.model.entity.Model;
import org.elmo.robella.model.entity.VendorModel;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ModelService {
    private final ModelRepository modelRepository;

    public final VendorModelRepository vendorModelRepository;

    public ModelService(ModelRepository modelRepository, VendorModelRepository vendorModelRepository) {
        this.modelRepository = modelRepository;
        this.vendorModelRepository = vendorModelRepository;
    }

    public Flux<Model> getModels() {
        return modelRepository.findAll();
    }

    public Flux<Model> getPublishedModels() {
        return modelRepository.findByPublishedTrue();
    }

    public Flux<Model> getModelsByOrganization(String organization) {
        return modelRepository.findByOrganization(organization);
    }

    public Mono<Model> createModel(Model model) {
        return modelRepository.save(model);
    }

    public Mono<Model> updateModel(Model model) {
        if (model.getId() == null) {
            return Mono.error(new IllegalArgumentException("Model ID cannot be null"));
        }
        
        return modelRepository.findById(model.getId())
                .flatMap(existingModel -> {
                    // 更新所有字段
                    existingModel.setName(model.getName());
                    existingModel.setModelKey(model.getModelKey());
                    existingModel.setDescription(model.getDescription());
                    existingModel.setOrganization(model.getOrganization());
                    existingModel.setCapabilities(model.getCapabilities());
                    existingModel.setContextWindow(model.getContextWindow());
                    existingModel.setPublished(model.getPublished());
                    return modelRepository.save(existingModel);
                })
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Model not found")));
    }


    public Mono<Void> deleteModel(Long id) {
        return modelRepository.deleteById(id);
    }

    public Mono<Model> getModelById(Long id) {
        return modelRepository.findById(id);
    }

    public Mono<Model> getModelByModelKey(String modelKey) {
        return modelRepository.findByModelKey(modelKey);
    }

    public Flux<VendorModel> getVendorModelsByModelId(Long modelId) {
        return vendorModelRepository.findByModelId(modelId);
    }

    // 新增功能：状态管理
    public Mono<Model> publishModel(Long id) {
        return modelRepository.findById(id)
                .flatMap(model -> {
                    model.setPublished(true);
                    return modelRepository.save(model);
                })
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Model not found")));
    }

    public Mono<Model> unpublishModel(Long id) {
        return modelRepository.findById(id)
                .flatMap(model -> {
                    model.setPublished(false);
                    return modelRepository.save(model);
                })
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Model not found")));
    }

    // 新增功能：搜索和筛选
    public Flux<Model> searchModelsByName(String keyword) {
        return modelRepository.findAll()
                .filter(model -> model.getName() != null && 
                        model.getName().toLowerCase().contains(keyword.toLowerCase()));
    }

    public Flux<Model> getModelsByCapability(String capability) {
        return modelRepository.findAll()
                .filter(model -> model.getCapabilities() != null && 
                        model.getCapabilities().stream()
                                .anyMatch(cap -> cap.name().equalsIgnoreCase(capability)));
    }

    // 新增功能：统计信息
    public Mono<Long> countTotalModels() {
        return modelRepository.count();
    }

    public Mono<Long> countPublishedModels() {
        return modelRepository.findByPublishedTrue().count();
    }

    public Mono<Long> countModelsByOrganization(String organization) {
        return modelRepository.findByOrganization(organization).count();
    }

    // 新增功能：模型验证
    public Mono<Boolean> modelExistsByName(String name) {
        return modelRepository.findByName(name)
                .map(model -> true)
                .defaultIfEmpty(false);
    }

    public Mono<Boolean> modelExistsByModelKey(String modelKey) {
        return modelRepository.findByModelKey(modelKey)
                .map(model -> true)
                .defaultIfEmpty(false);
    }

    public Mono<Model> validateAndCreateModel(Model model) {
        return modelExistsByName(model.getName())
                .flatMap(nameExists -> {
                    if (nameExists) {
                        return Mono.error(new IllegalArgumentException("Model name already exists"));
                    }
                    return modelExistsByModelKey(model.getModelKey())
                            .flatMap(keyExists -> {
                                if (keyExists) {
                                    return Mono.error(new IllegalArgumentException("Model key already exists"));
                                }
                                return createModel(model);
                            });
                });
    }

    // 修正方法名的拼写错误
    public Mono<VendorModel> addVendorModelForModel(Long modelId, Long vendorModelId) {
        return vendorModelRepository.findById(vendorModelId)
                .flatMap(vendorModel -> {
                    vendorModel.setModelId(modelId);
                    return vendorModelRepository.save(vendorModel);
                });
    }

}

package org.elmo.robella.controller;

import org.elmo.robella.model.entity.Model;
import org.elmo.robella.model.entity.VendorModel;
import org.elmo.robella.service.ModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@RestController
@RequestMapping("/api/models")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ModelController {
    
    private final ModelService modelService;
    
    // 基本 CRUD 操作
    @GetMapping
    public Flux<Model> getAllModels() {
        return modelService.getModels()
                .doOnError(error -> log.error("获取模型列表失败: {}", error.getMessage()));
    }
    
    @GetMapping("/published")
    public Flux<Model> getPublishedModels() {
        return modelService.getPublishedModels()
                .doOnError(error -> log.error("获取已发布模型列表失败: {}", error.getMessage()));
    }
    
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Model>> getModelById(@PathVariable @NotNull Long id) {
        return modelService.getModelById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnError(error -> log.error("获取模型失败 [ID: {}]: {}", id, error.getMessage()));
    }
    
    @PostMapping
    public Mono<ResponseEntity<Model>> createModel(@Valid @RequestBody Model model) {
        return modelService.validateAndCreateModel(model)
                .map(savedModel -> ResponseEntity.status(HttpStatus.CREATED).body(savedModel))
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("创建模型失败: {}", e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().build());
                })
                .doOnError(error -> log.error("创建模型失败: {}", error.getMessage()));
    }
    
    @PutMapping("/{id}")
    public Mono<ResponseEntity<Model>> updateModel(@PathVariable @NotNull Long id, @Valid @RequestBody Model model) {
        model.setId(id);
        return modelService.updateModel(model)
                .map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("更新模型失败 [ID: {}]: {}", id, e.getMessage());
                    return Mono.just(ResponseEntity.notFound().build());
                })
                .doOnError(error -> log.error("更新模型失败 [ID: {}]: {}", id, error.getMessage()));
    }
    
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteModel(@PathVariable @NotNull Long id) {
        return modelService.deleteModel(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .doOnError(error -> log.error("删除模型失败 [ID: {}]: {}", id, error.getMessage()));
    }
    
    // 状态管理
    @PutMapping("/{id}/publish")
    public Mono<ResponseEntity<Model>> publishModel(@PathVariable @NotNull Long id) {
        return modelService.publishModel(id)
                .map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("发布模型失败 [ID: {}]: {}", id, e.getMessage());
                    return Mono.just(ResponseEntity.notFound().build());
                })
                .doOnError(error -> log.error("发布模型失败 [ID: {}]: {}", id, error.getMessage()));
    }
    
    @PutMapping("/{id}/unpublish")
    public Mono<ResponseEntity<Model>> unpublishModel(@PathVariable @NotNull Long id) {
        return modelService.unpublishModel(id)
                .map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("取消发布模型失败 [ID: {}]: {}", id, e.getMessage());
                    return Mono.just(ResponseEntity.notFound().build());
                })
                .doOnError(error -> log.error("取消发布模型失败 [ID: {}]: {}", id, error.getMessage()));
    }
    
    // 搜索和筛选
    @GetMapping("/search")
    public Flux<Model> searchModels(@RequestParam String keyword) {
        return modelService.searchModelsByName(keyword)
                .doOnError(error -> log.error("搜索模型失败 [keyword: {}]: {}", keyword, error.getMessage()));
    }
    
    @GetMapping("/organization/{organization}")
    public Flux<Model> getModelsByOrganization(@PathVariable String organization) {
        return modelService.getModelsByOrganization(organization)
                .doOnError(error -> log.error("获取组织模型失败 [organization: {}]: {}", organization, error.getMessage()));
    }
    
    @GetMapping("/capability/{capability}")
    public Flux<Model> getModelsByCapability(@PathVariable String capability) {
        return modelService.getModelsByCapability(capability)
                .doOnError(error -> log.error("按能力筛选模型失败 [capability: {}]: {}", capability, error.getMessage()));
    }
    
    // 统计信息
    @GetMapping("/stats")
    public Mono<ResponseEntity<Map<String, Object>>> getModelStats() {
        return Mono.zip(
                modelService.countTotalModels(),
                modelService.countPublishedModels()
        ).map(tuple -> {
            Map<String, Object> stats = Map.of(
                "totalModels", tuple.getT1(),
                "publishedModels", tuple.getT2()
            );
            return ResponseEntity.ok(stats);
        }).doOnError(error -> log.error("获取模型统计信息失败: {}", error.getMessage()));
    }
    
    @GetMapping("/stats/organization/{organization}")
    public Mono<ResponseEntity<Long>> getModelCountByOrganization(@PathVariable String organization) {
        return modelService.countModelsByOrganization(organization)
                .map(ResponseEntity::ok)
                .doOnError(error -> log.error("获取组织模型统计失败 [organization: {}]: {}", organization, error.getMessage()));
    }
    
    // VendorModel 管理
    @GetMapping("/{id}/vendor-models")
    public Flux<VendorModel> getVendorModelsByModelId(@PathVariable @NotNull Long id) {
        return modelService.getVendorModelsByModelId(id)
                .doOnError(error -> log.error("获取模型关联的 VendorModel 失败 [modelId: {}]: {}", id, error.getMessage()));
    }
    
    @PostMapping("/{modelId}/vendor-models/{vendorModelId}")
    public Mono<ResponseEntity<VendorModel>> addVendorModelToModel(
            @PathVariable @NotNull Long modelId, 
            @PathVariable @NotNull Long vendorModelId) {
        return modelService.addVendorModelForModel(modelId, vendorModelId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnError(error -> log.error("添加 VendorModel 到模型失败 [modelId: {}, vendorModelId: {}]: {}", 
                    modelId, vendorModelId, error.getMessage()));
    }
    
    // 验证接口
    @GetMapping("/exists/{name}")
    public Mono<ResponseEntity<Boolean>> checkModelExists(@PathVariable String name) {
        return modelService.modelExistsByName(name)
                .map(ResponseEntity::ok)
                .doOnError(error -> log.error("检查模型名称是否存在失败 [name: {}]: {}", name, error.getMessage()));
    }
}

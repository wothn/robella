package org.elmo.robella.controller;

import org.elmo.robella.annotation.RequiredRole;
import org.elmo.robella.model.common.Role;
import org.elmo.robella.model.entity.Model;
import org.elmo.robella.model.entity.VendorModel;
import org.elmo.robella.service.ModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
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
    public List<Model> getAllModels() {
        try {
            return modelService.getModels();
        } catch (Exception error) {
            log.error("获取模型列表失败: {}", error.getMessage());
            throw new RuntimeException("获取模型列表失败", error);
        }
    }

    @GetMapping("/published")
    public List<Model> getPublishedModels() {
        try {
            return modelService.getPublishedModels();
        } catch (Exception error) {
            log.error("获取已发布模型列表失败: {}", error.getMessage());
            throw new RuntimeException("获取已发布模型列表失败", error);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Model> getModelById(@PathVariable @NotNull Long id) {
        try {
            Model model = modelService.getModelById(id);
            if (model == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(model);
        } catch (Exception error) {
            log.error("获取模型失败 [ID: {}]: {}", id, error.getMessage());
            throw new RuntimeException("获取模型失败", error);
        }
    }

    @GetMapping("/key/{modelKey}")
    public ResponseEntity<Model> getModelByModelKey(@PathVariable @NotNull String modelKey) {
        try {
            Model model = modelService.getModelByModelKey(modelKey);
            if (model == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(model);
        } catch (Exception error) {
            log.error("获取模型失败 [modelKey: {}]: {}", modelKey, error.getMessage());
            throw new RuntimeException("获取模型失败", error);
        }
    }

    @PostMapping
    @RequiredRole(Role.ROOT)
    public ResponseEntity<Model> createModel(@Valid @RequestBody Model model) {
        try {
            int result = modelService.validateAndCreateModel(model);
            if (result > 0) {
                return ResponseEntity.status(HttpStatus.CREATED).body(model);
            }
            return ResponseEntity.badRequest().build();
        } catch (IllegalArgumentException e) {
            log.warn("创建模型失败: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception error) {
            log.error("创建模型失败: {}", error.getMessage());
            throw new RuntimeException("创建模型失败", error);
        }
    }

    @PutMapping("/{id}")
    @RequiredRole(Role.ROOT)
    public ResponseEntity<Model> updateModel(@PathVariable @NotNull Long id, @Valid @RequestBody Model model) {
        try {
            model.setId(id);
            int result = modelService.updateModel(model);
            if (result > 0) {
                return ResponseEntity.ok(model);
            }
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            log.warn("更新模型失败 [ID: {}]: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception error) {
            log.error("更新模型失败 [ID: {}]: {}", id, error.getMessage());
            throw new RuntimeException("更新模型失败", error);
        }
    }

    @DeleteMapping("/{id}")
    @RequiredRole(Role.ROOT)
    public ResponseEntity<Void> deleteModel(@PathVariable @NotNull Long id) {
        try {
            int result = modelService.deleteModel(id);
            if (result > 0) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.notFound().build();
        } catch (Exception error) {
            log.error("删除模型失败 [ID: {}]: {}", id, error.getMessage());
            throw new RuntimeException("删除模型失败", error);
        }
    }

    // 状态管理
    @PutMapping("/{id}/publish")
    @RequiredRole(Role.ROOT)
    public ResponseEntity<Model> publishModel(@PathVariable @NotNull Long id) {
        try {
            int result = modelService.publishModel(id);
            if (result > 0) {
                Model model = modelService.getModelById(id);
                return ResponseEntity.ok(model);
            }
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            log.warn("发布模型失败 [ID: {}]: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception error) {
            log.error("发布模型失败 [ID: {}]: {}", id, error.getMessage());
            throw new RuntimeException("发布模型失败", error);
        }
    }

    @PutMapping("/{id}/unpublish")
    @RequiredRole(Role.ROOT)
    public ResponseEntity<Model> unpublishModel(@PathVariable @NotNull Long id) {
        try {
            int result = modelService.unpublishModel(id);
            if (result > 0) {
                Model model = modelService.getModelById(id);
                return ResponseEntity.ok(model);
            }
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            log.warn("取消发布模型失败 [ID: {}]: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception error) {
            log.error("取消发布模型失败 [ID: {}]: {}", id, error.getMessage());
            throw new RuntimeException("取消发布模型失败", error);
        }
    }

    // 搜索和筛选
    @GetMapping("/search")
    public List<Model> searchModels(@RequestParam String keyword) {
        try {
            return modelService.searchModelsByName(keyword);
        } catch (Exception error) {
            log.error("搜索模型失败 [keyword: {}]: {}", keyword, error.getMessage());
            throw new RuntimeException("搜索模型失败", error);
        }
    }

    @GetMapping("/organization/{organization}")
    public List<Model> getModelsByOrganization(@PathVariable String organization) {
        try {
            return modelService.getModelsByOrganization(organization);
        } catch (Exception error) {
            log.error("获取组织模型失败 [organization: {}]: {}", organization, error.getMessage());
            throw new RuntimeException("获取组织模型失败", error);
        }
    }

    @GetMapping("/capability/{capability}")
    public List<Model> getModelsByCapability(@PathVariable String capability) {
        try {
            return modelService.getModelsByCapability(capability);
        } catch (Exception error) {
            log.error("按能力筛选模型失败 [capability: {}]: {}", capability, error.getMessage());
            throw new RuntimeException("按能力筛选模型失败", error);
        }
    }

    // 统计信息
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getModelStats() {
        try {
            long totalModels = modelService.countTotalModels();
            long publishedModels = modelService.countPublishedModels();

            Map<String, Object> stats = Map.of(
                "totalModels", totalModels,
                "publishedModels", publishedModels
            );
            return ResponseEntity.ok(stats);
        } catch (Exception error) {
            log.error("获取模型统计信息失败: {}", error.getMessage());
            throw new RuntimeException("获取模型统计信息失败", error);
        }
    }

    @GetMapping("/stats/organization/{organization}")
    public ResponseEntity<Long> getModelCountByOrganization(@PathVariable String organization) {
        try {
            long count = modelService.countModelsByOrganization(organization);
            return ResponseEntity.ok(count);
        } catch (Exception error) {
            log.error("获取组织模型统计失败 [organization: {}]: {}", organization, error.getMessage());
            throw new RuntimeException("获取组织模型统计失败", error);
        }
    }

    // VendorModel 管理
    @GetMapping("/{id}/vendor-models")
    public List<VendorModel> getVendorModelsByModelId(@PathVariable @NotNull Long id) {
        try {
            return modelService.getVendorModelsByModelId(id);
        } catch (Exception error) {
            log.error("获取模型关联的 VendorModel 失败 [modelId: {}]: {}", id, error.getMessage());
            throw new RuntimeException("获取模型关联的 VendorModel 失败", error);
        }
    }

    @PostMapping("/{modelId}/vendor-models/{vendorModelId}")
    @RequiredRole(Role.ROOT)
    public ResponseEntity<VendorModel> addVendorModelToModel(
            @PathVariable @NotNull Long modelId,
            @PathVariable @NotNull Long vendorModelId) {
        try {
            int result = modelService.addVendorModelForModel(modelId, vendorModelId);
            if (result > 0) {
                VendorModel vendorModel = modelService.getVendorModelsByModelId(modelId)
                    .stream()
                    .filter(vm -> vm.getId().equals(vendorModelId))
                    .findFirst()
                    .orElse(null);
                return ResponseEntity.ok(vendorModel);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception error) {
            log.error("添加 VendorModel 到模型失败 [modelId: {}, vendorModelId: {}]: {}",
                modelId, vendorModelId, error.getMessage());
            throw new RuntimeException("添加 VendorModel 到模型失败", error);
        }
    }

    // 验证接口
    @GetMapping("/exists/name/{name}")
    public ResponseEntity<Boolean> checkModelNameExists(@PathVariable String name) {
        try {
            boolean exists = modelService.modelExistsByName(name);
            return ResponseEntity.ok(exists);
        } catch (Exception error) {
            log.error("检查模型名称是否存在失败 [name: {}]: {}", name, error.getMessage());
            throw new RuntimeException("检查模型名称是否存在失败", error);
        }
    }

    @GetMapping("/exists/key/{modelKey}")
    public ResponseEntity<Boolean> checkModelKeyExists(@PathVariable String modelKey) {
        try {
            boolean exists = modelService.modelExistsByModelKey(modelKey);
            return ResponseEntity.ok(exists);
        } catch (Exception error) {
            log.error("检查模型调用标识是否存在失败 [modelKey: {}]: {}", modelKey, error.getMessage());
            throw new RuntimeException("检查模型调用标识是否存在失败", error);
        }
    }
}

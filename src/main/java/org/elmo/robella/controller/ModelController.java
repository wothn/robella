package org.elmo.robella.controller;

import org.elmo.robella.annotation.RequiredRole;
import org.elmo.robella.model.common.Role;
import org.elmo.robella.model.entity.Model;
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
    @RequiredRole(Role.ADMIN)
    public List<Model> getAllModels() {
        return modelService.list();
    }

    @GetMapping("/published")
    public List<Model> getPublishedModels() {
        return modelService.findByPublishedTrue();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Model> getModelById(@PathVariable @NotNull Long id) {
        Model model = modelService.getById(id);
        if (model == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(model);
    }

    @PostMapping
    @RequiredRole(Role.ROOT)
    public ResponseEntity<Model> createModel(@Valid @RequestBody Model model) {
        boolean saved = modelService.save(model);
        if (saved) {
            return ResponseEntity.status(HttpStatus.CREATED).body(model);
        }
        return ResponseEntity.badRequest().build();
    }

    @PutMapping("/{id}")
    @RequiredRole(Role.ROOT)
    public ResponseEntity<Model> updateModel(@PathVariable @NotNull Long id, @Valid @RequestBody Model model) {
        model.setId(id);
        boolean updated = modelService.updateById(model);
        if (updated) {
            return ResponseEntity.ok(model);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @RequiredRole(Role.ROOT)
    public ResponseEntity<Void> deleteModel(@PathVariable @NotNull Long id) {
        boolean deleted = modelService.removeById(id);
        if (deleted) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // 状态管理
    @PutMapping("/{id}/publish")
    @RequiredRole(Role.ROOT)
    public ResponseEntity<Model> publishModel(@PathVariable @NotNull Long id) {
        Model model = modelService.getById(id);
        if (model == null) {
            return ResponseEntity.notFound().build();
        }
        model.setPublished(true);
        boolean updated = modelService.updateById(model);
        if (updated) {
            return ResponseEntity.ok(model);
        }
        return ResponseEntity.internalServerError().build();
    }

    @PutMapping("/{id}/unpublish")
    @RequiredRole(Role.ROOT)
    public ResponseEntity<Model> unpublishModel(@PathVariable @NotNull Long id) {
        Model model = modelService.getById(id);
        if (model == null) {
            return ResponseEntity.notFound().build();
        }
        model.setPublished(false);
        boolean updated = modelService.updateById(model);
        if (updated) {
            return ResponseEntity.ok(model);
        }
        return ResponseEntity.internalServerError().build();
    }

    // 搜索和筛选
    @GetMapping("/search")
    public List<Model> searchModels(@RequestParam String keyword) {
        return modelService.searchModels(keyword);
    }

    @GetMapping("/capability/{capability}")
    public List<Model> getModelsByCapability(@PathVariable String capability) {
        return modelService.findByCapability(capability);
    }

}

package org.elmo.robella.controller;

import org.elmo.robella.annotation.RequiredRole;
import org.elmo.robella.exception.ValidationException;
import org.elmo.robella.model.common.Role;
import org.elmo.robella.model.dto.VendorModelDTO;
import org.elmo.robella.service.VendorModelService;
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
@RequestMapping("/api/vendor-models")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
@Validated
public class VendorModelController {

    private final VendorModelService vendorModelService;

    // 基本查询接口
    @GetMapping
    @RequiredRole(Role.ADMIN)
    public ResponseEntity<List<VendorModelDTO>> getAllVendorModels() {
        List<VendorModelDTO> vendorModels = vendorModelService.getAllVendorModels();
        return ResponseEntity.ok(vendorModels);
    }

    @GetMapping("/enabled")
    public ResponseEntity<List<VendorModelDTO>> getEnabledVendorModels() {
        List<VendorModelDTO> vendorModels = vendorModelService.getEnabledVendorModels();
        return ResponseEntity.ok(vendorModels);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendorModelDTO> getVendorModelById(@PathVariable @NotNull Long id) {
        VendorModelDTO vendorModel = vendorModelService.getVendorModelById(id);
        if (vendorModel == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(vendorModel);
    }

    @GetMapping("/model/{modelId}")
    public ResponseEntity<List<VendorModelDTO>> getVendorModelsByModelId(@PathVariable @NotNull Long modelId) {
        List<VendorModelDTO> vendorModels = vendorModelService.getVendorModelsByModelId(modelId);
        return ResponseEntity.ok(vendorModels);
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<VendorModelDTO>> getVendorModelsByProviderId(@PathVariable @NotNull Long providerId) {
        List<VendorModelDTO> vendorModels = vendorModelService.getVendorModelsByProviderId(providerId);
        return ResponseEntity.ok(vendorModels);
    }

    @GetMapping("/model-key/{modelKey}")
    public ResponseEntity<List<VendorModelDTO>> getEnabledVendorModelsByModelKey(@PathVariable @NotNull String modelKey) {
        List<VendorModelDTO> vendorModels = vendorModelService.getEnabledVendorModelsByModelKey(modelKey);
        return ResponseEntity.ok(vendorModels);
    }

    @GetMapping("/vendor-key/{vendorModelKey}")
    public ResponseEntity<VendorModelDTO> findByVendorModelKey(@PathVariable @NotNull String vendorModelKey) {
        VendorModelDTO vendorModel = vendorModelService.findByVendorModelKey(vendorModelKey);
        if (vendorModel == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(vendorModel);
    }

    @GetMapping("/vendor-key/{vendorModelKey}/enabled")
    public ResponseEntity<VendorModelDTO> findEnabledByVendorModelKey(@PathVariable @NotNull String vendorModelKey) {
        VendorModelDTO vendorModel = vendorModelService.findEnabledByVendorModelKey(vendorModelKey);
        if (vendorModel == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(vendorModel);
    }

    // 创建和更新接口
    @PostMapping
    @RequiredRole(Role.ROOT)
    public ResponseEntity<VendorModelDTO> createVendorModel(@Valid @RequestBody VendorModelDTO.CreateRequest request) {
        try {
            VendorModelDTO created = vendorModelService.createVendorModel(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (ValidationException e) {
            log.warn("Validation error creating vendor model: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error creating vendor model", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    @RequiredRole(Role.ROOT)
    public ResponseEntity<VendorModelDTO> updateVendorModel(
            @PathVariable @NotNull Long id,
            @Valid @RequestBody VendorModelDTO.UpdateRequest request) {
        try {
            VendorModelDTO updated = vendorModelService.updateVendorModel(id, request);
            return ResponseEntity.ok(updated);
        } catch (ValidationException e) {
            log.warn("Validation error updating vendor model {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error updating vendor model {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 删除接口
    @DeleteMapping("/{id}")
    @RequiredRole(Role.ROOT)
    public ResponseEntity<Void> deleteVendorModel(@PathVariable @NotNull Long id) {
        try {
            boolean deleted = vendorModelService.deleteVendorModel(id);
            if (deleted) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error deleting vendor model {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 状态管理接口
    @PutMapping("/{id}/enable")
    @RequiredRole(Role.ROOT)
    public ResponseEntity<VendorModelDTO> enableVendorModel(@PathVariable @NotNull Long id) {
        try {
            VendorModelDTO updated = vendorModelService.toggleVendorModelStatus(id, true);
            return ResponseEntity.ok(updated);
        } catch (ValidationException e) {
            log.warn("Validation error enabling vendor model {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error enabling vendor model {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}/disable")
    @RequiredRole(Role.ROOT)
    public ResponseEntity<VendorModelDTO> disableVendorModel(@PathVariable @NotNull Long id) {
        try {
            VendorModelDTO updated = vendorModelService.toggleVendorModelStatus(id, false);
            return ResponseEntity.ok(updated);
        } catch (ValidationException e) {
            log.warn("Validation error disabling vendor model {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error disabling vendor model {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
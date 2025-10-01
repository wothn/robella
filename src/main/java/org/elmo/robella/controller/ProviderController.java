package org.elmo.robella.controller;

import org.elmo.robella.annotation.RequiredRole;
import org.elmo.robella.model.common.Role;
import org.elmo.robella.model.entity.Provider;
import org.elmo.robella.service.ProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/providers")
public class ProviderController {

    @Autowired
    private ProviderService providerService;

    @GetMapping
    public List<Provider> getAllProviders() {
        return providerService.getAllProviders();
    }

    @GetMapping("/active")
    @RequiredRole(Role.ROOT)
    public List<Provider> getActiveProviders() {
        return providerService.getEnabledProviders();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Provider> getProviderById(@PathVariable Long id) {
        Provider provider = providerService.getProviderById(id);
        if (provider != null) {
            return ResponseEntity.ok(provider);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @RequiredRole(Role.ROOT)
    public ResponseEntity<Provider> createProvider(@RequestBody Provider provider) {
        boolean created = providerService.createProvider(provider);
        if (created) {
            return ResponseEntity.ok(provider);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @RequiredRole(Role.ROOT)
    public ResponseEntity<Provider> updateProvider(@PathVariable Long id, @RequestBody Provider provider) {
        boolean updated = providerService.updateProvider(id, provider);
        if (updated) {
            return ResponseEntity.ok(provider);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @RequiredRole(Role.ROOT)
    public ResponseEntity<Void> deleteProvider(@PathVariable Long id) {
        providerService.deleteProvider(id);
        return ResponseEntity.ok().build();
    }

}
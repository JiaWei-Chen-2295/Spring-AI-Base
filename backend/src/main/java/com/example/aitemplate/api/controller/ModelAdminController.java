package com.example.aitemplate.api.controller;

import com.example.aitemplate.api.dto.ModelAdminInfo;
import com.example.aitemplate.api.dto.ModelToggleResponse;
import com.example.aitemplate.api.dto.ModelUpsertRequest;
import com.example.aitemplate.app.ModelRegistry;
import com.example.aitemplate.core.model.CapabilitySet;
import com.example.aitemplate.core.model.ModelConfig;
import com.example.aitemplate.plugins.model.DynamicModelAdapter;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/models")
@Validated
public class ModelAdminController {

    private final ModelRegistry modelRegistry;

    public ModelAdminController(ModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry;
    }

    @GetMapping
    public List<ModelAdminInfo> list() {
        return modelRegistry.listAll().stream()
                .map(entry -> new ModelAdminInfo(
                        entry.adapter().modelId(),
                        entry.adapter().provider(),
                        resolveDisplayName(entry),
                        entry.enabled(),
                        entry.source(),
                        entry.editable(),
                        entry.adapter().capabilities(),
                        entry.adapter().health()))
                .toList();
    }

    @PostMapping
    public ModelAdminInfo upsert(@Valid @RequestBody ModelUpsertRequest request) {
        CapabilitySet caps = request.capabilities() != null
                ? request.capabilities()
                : new CapabilitySet(true, true, true, false);
        ModelConfig config = new ModelConfig(
                request.modelId(),
                request.provider() != null ? request.provider() : "openai",
                request.displayName() != null ? request.displayName() : request.modelId(),
                request.baseUrl(),
                request.apiKey(),
                request.modelName(),
                true,
                caps,
                request.sortOrder() != null ? request.sortOrder() : 100);
        DynamicModelAdapter adapter = modelRegistry.upsertDynamic(config);
        return new ModelAdminInfo(
                adapter.modelId(), adapter.provider(),
                config.displayName(), true, "dynamic", true,
                adapter.capabilities(), adapter.health());
    }

    @DeleteMapping
    public void delete(@RequestParam String modelId) {
        if (!modelRegistry.deleteDynamic(modelId)) {
            throw new IllegalArgumentException(
                    "Dynamic model not found or is builtin (cannot delete): " + modelId);
        }
    }

    @PatchMapping("/{modelId}/toggle")
    public ModelToggleResponse toggle(@PathVariable String modelId) {
        boolean nowEnabled = modelRegistry.toggleEnabled(modelId);
        return new ModelToggleResponse(modelId, nowEnabled);
    }

    private String resolveDisplayName(ModelRegistry.ModelEntry entry) {
        if (entry.adapter() instanceof DynamicModelAdapter dynamic) {
            return dynamic.config().displayName();
        }
        return entry.adapter().modelId();
    }
}

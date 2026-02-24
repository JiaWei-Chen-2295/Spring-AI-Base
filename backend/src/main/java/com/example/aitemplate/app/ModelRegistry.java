package com.example.aitemplate.app;

import com.example.aitemplate.core.model.ModelAdapter;
import com.example.aitemplate.core.model.ModelConfig;
import com.example.aitemplate.infra.db.ModelConfigRepository;
import com.example.aitemplate.plugins.model.DynamicModelAdapter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ModelRegistry {

    private static final Logger log = LoggerFactory.getLogger(ModelRegistry.class);

    private final Map<String, ModelAdapter> builtinAdapters;
    private final Map<String, DynamicModelAdapter> dynamicAdapters;
    private final Set<String> disabledModelIds;
    private final ModelConfigRepository modelConfigRepo;

    public ModelRegistry(List<ModelAdapter> adapters, ModelConfigRepository modelConfigRepo) {
        this.builtinAdapters = new ConcurrentHashMap<>();
        this.dynamicAdapters = new ConcurrentHashMap<>();
        this.disabledModelIds = ConcurrentHashMap.newKeySet();
        this.modelConfigRepo = modelConfigRepo;

        for (ModelAdapter adapter : adapters) {
            this.builtinAdapters.put(adapter.modelId(), adapter);
        }

        loadFromDb();
    }

    /** Returns only enabled models — used by ChatService and MetadataController. */
    public List<ModelAdapter> list() {
        List<ModelAdapter> result = new ArrayList<>();
        builtinAdapters.values().stream()
                .filter(a -> !disabledModelIds.contains(a.modelId()))
                .sorted(Comparator.comparing(ModelAdapter::modelId, String.CASE_INSENSITIVE_ORDER))
                .forEach(result::add);
        dynamicAdapters.values().stream()
                .filter(a -> !disabledModelIds.contains(a.modelId()))
                .sorted(Comparator.comparing(ModelAdapter::modelId, String.CASE_INSENSITIVE_ORDER))
                .forEach(result::add);
        return result;
    }

    /** Returns all models (builtin + dynamic, enabled + disabled) for admin. */
    public List<ModelEntry> listAll() {
        List<ModelEntry> entries = new ArrayList<>();
        builtinAdapters.values().stream()
                .sorted(Comparator.comparing(ModelAdapter::modelId, String.CASE_INSENSITIVE_ORDER))
                .forEach(a -> entries.add(new ModelEntry(
                        a, "builtin", false, !disabledModelIds.contains(a.modelId()))));
        dynamicAdapters.values().stream()
                .sorted(Comparator.comparing(ModelAdapter::modelId, String.CASE_INSENSITIVE_ORDER))
                .forEach(a -> entries.add(new ModelEntry(
                        a, "dynamic", true, !disabledModelIds.contains(a.modelId()))));
        return entries;
    }

    public ModelAdapter getOrThrow(String modelId) {
        ModelAdapter adapter = builtinAdapters.get(modelId);
        if (adapter == null) {
            adapter = dynamicAdapters.get(modelId);
        }
        if (adapter == null) {
            throw new IllegalArgumentException("Unknown modelId: " + modelId);
        }
        if (disabledModelIds.contains(modelId)) {
            throw new IllegalArgumentException("Model is disabled: " + modelId);
        }
        return adapter;
    }

    public DynamicModelAdapter upsertDynamic(ModelConfig config) {
        validateConfig(config);
        DynamicModelAdapter adapter = new DynamicModelAdapter(config);
        dynamicAdapters.put(config.modelId(), adapter);
        if (!config.enabled()) {
            disabledModelIds.add(config.modelId());
        } else {
            disabledModelIds.remove(config.modelId());
        }
        modelConfigRepo.save(config);
        return adapter;
    }

    public boolean deleteDynamic(String modelId) {
        DynamicModelAdapter removed = dynamicAdapters.remove(modelId);
        if (removed != null) {
            disabledModelIds.remove(modelId);
            modelConfigRepo.delete(modelId);
            return true;
        }
        return false;
    }

    public boolean toggleEnabled(String modelId) {
        if (!builtinAdapters.containsKey(modelId) && !dynamicAdapters.containsKey(modelId)) {
            throw new IllegalArgumentException("Unknown modelId: " + modelId);
        }
        boolean nowEnabled;
        if (disabledModelIds.contains(modelId)) {
            disabledModelIds.remove(modelId);
            nowEnabled = true;
        } else {
            disabledModelIds.add(modelId);
            nowEnabled = false;
        }
        DynamicModelAdapter dynamic = dynamicAdapters.get(modelId);
        if (dynamic != null) {
            ModelConfig old = dynamic.config();
            ModelConfig updated = new ModelConfig(
                    old.modelId(), old.provider(), old.displayName(),
                    old.baseUrl(), old.apiKey(), old.modelName(),
                    nowEnabled, old.capabilities(), old.sortOrder());
            modelConfigRepo.save(updated);
        }
        return nowEnabled;
    }

    // ── DB persistence ──────────────────────────────────────

    private void loadFromDb() {
        List<ModelConfig> configs = modelConfigRepo.findAll();
        for (ModelConfig config : configs) {
            DynamicModelAdapter adapter = new DynamicModelAdapter(config);
            dynamicAdapters.put(config.modelId(), adapter);
            if (!config.enabled()) {
                disabledModelIds.add(config.modelId());
            }
            log.info("Loaded dynamic model from DB: {}", config.modelId());
        }
    }

    private void validateConfig(ModelConfig config) {
        if (config.modelId() == null || config.modelId().isBlank()) {
            throw new IllegalArgumentException("modelId is required");
        }
        if (config.baseUrl() == null || config.baseUrl().isBlank()) {
            throw new IllegalArgumentException("baseUrl is required");
        }
        if (config.modelName() == null || config.modelName().isBlank()) {
            throw new IllegalArgumentException("modelName is required");
        }
        if (builtinAdapters.containsKey(config.modelId())) {
            throw new IllegalArgumentException(
                    "Cannot create dynamic model with same ID as builtin: " + config.modelId());
        }
    }

    public record ModelEntry(ModelAdapter adapter, String source, boolean editable, boolean enabled) {}
}

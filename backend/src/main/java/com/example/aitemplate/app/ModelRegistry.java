package com.example.aitemplate.app;

import com.example.aitemplate.core.model.ModelAdapter;
import com.example.aitemplate.core.model.ModelConfig;
import com.example.aitemplate.plugins.model.DynamicModelAdapter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ModelRegistry {

    private static final Logger log = LoggerFactory.getLogger(ModelRegistry.class);

    private final Map<String, ModelAdapter> builtinAdapters;
    private final Map<String, DynamicModelAdapter> dynamicAdapters;
    private final Set<String> disabledModelIds;
    private final Path modelConfigRoot;
    private final ObjectMapper objectMapper;

    public ModelRegistry(
            List<ModelAdapter> adapters,
            @Value("${app.models.config-dir:models/runtime}") String configDir,
            ObjectMapper objectMapper) {
        this.builtinAdapters = new ConcurrentHashMap<>();
        this.dynamicAdapters = new ConcurrentHashMap<>();
        this.disabledModelIds = ConcurrentHashMap.newKeySet();
        this.objectMapper = objectMapper;
        this.modelConfigRoot = Paths.get(configDir).toAbsolutePath().normalize();

        for (ModelAdapter adapter : adapters) {
            this.builtinAdapters.put(adapter.modelId(), adapter);
        }

        loadFromDisk();
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
            DynamicModelAdapter dynamic = dynamicAdapters.get(modelId);
            adapter = dynamic;
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
        persistModelConfig(config);
        return adapter;
    }

    public boolean deleteDynamic(String modelId) {
        DynamicModelAdapter removed = dynamicAdapters.remove(modelId);
        if (removed != null) {
            disabledModelIds.remove(modelId);
            deleteModelConfigFiles(modelId);
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
        persistDisabledState();
        DynamicModelAdapter dynamic = dynamicAdapters.get(modelId);
        if (dynamic != null) {
            ModelConfig old = dynamic.config();
            ModelConfig updated = new ModelConfig(
                    old.modelId(), old.provider(), old.displayName(),
                    old.baseUrl(), old.apiKey(), old.modelName(),
                    nowEnabled, old.capabilities(), old.sortOrder());
            persistModelConfig(updated);
        }
        return nowEnabled;
    }

    // ── Filesystem persistence ──────────────────────────────

    private void loadFromDisk() {
        try {
            if (!Files.exists(modelConfigRoot)) {
                Files.createDirectories(modelConfigRoot);
                return;
            }
            loadDisabledState();
            try (var stream = Files.walk(modelConfigRoot)) {
                stream.filter(path -> path.getFileName().toString().equals("model.config.json"))
                        .forEach(this::loadOneModelConfig);
            }
        } catch (IOException ex) {
            log.error("Failed to initialize model config directory: {}", modelConfigRoot, ex);
        }
    }

    private void loadOneModelConfig(Path configPath) {
        try {
            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            ModelConfig config = objectMapper.readValue(json, ModelConfig.class);
            DynamicModelAdapter adapter = new DynamicModelAdapter(config);
            dynamicAdapters.put(config.modelId(), adapter);
            if (!config.enabled()) {
                disabledModelIds.add(config.modelId());
            }
            log.info("Loaded dynamic model: {}", config.modelId());
        } catch (Exception ex) {
            log.warn("Skipping broken model config: {}", configPath, ex);
        }
    }

    private void persistModelConfig(ModelConfig config) {
        try {
            Path dir = modelConfigRoot.resolve(sanitize(config.modelId()));
            Files.createDirectories(dir);
            Path configPath = dir.resolve("model.config.json");
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
            Files.writeString(configPath, json, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to persist model config: " + config.modelId(), ex);
        }
    }

    private void deleteModelConfigFiles(String modelId) {
        Path dir = modelConfigRoot.resolve(sanitize(modelId));
        try {
            if (!Files.exists(dir)) {
                return;
            }
            try (var stream = Files.walk(dir)) {
                stream.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                        .forEach(path -> {
                            try { Files.deleteIfExists(path); } catch (IOException ignore) {}
                        });
            }
        } catch (IOException ignore) {}
    }

    private void loadDisabledState() {
        Path disabledFile = modelConfigRoot.resolve("_disabled.json");
        if (!Files.exists(disabledFile)) {
            return;
        }
        try {
            String json = Files.readString(disabledFile, StandardCharsets.UTF_8);
            List<String> ids = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            disabledModelIds.addAll(ids);
        } catch (Exception ex) {
            log.warn("Failed to load disabled state: {}", disabledFile, ex);
        }
    }

    private void persistDisabledState() {
        try {
            Files.createDirectories(modelConfigRoot);
            Path disabledFile = modelConfigRoot.resolve("_disabled.json");
            String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(List.copyOf(disabledModelIds));
            Files.writeString(disabledFile, json, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.warn("Failed to persist disabled state", ex);
        }
    }

    private void validateConfig(ModelConfig config) {
        if (config.modelId() == null || config.modelId().isBlank()) {
            throw new IllegalArgumentException("modelId is required");
        }
        if (config.baseUrl() == null || config.baseUrl().isBlank()) {
            throw new IllegalArgumentException("baseUrl is required");
        }
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            throw new IllegalArgumentException("apiKey is required");
        }
        if (config.modelName() == null || config.modelName().isBlank()) {
            throw new IllegalArgumentException("modelName is required");
        }
        if (builtinAdapters.containsKey(config.modelId())) {
            throw new IllegalArgumentException(
                    "Cannot create dynamic model with same ID as builtin: " + config.modelId());
        }
    }

    private String sanitize(String value) {
        String s = value == null ? "" : value.replaceAll("[^A-Za-z0-9._-]", "_");
        return s.isBlank() ? "_" : s;
    }

    public record ModelEntry(ModelAdapter adapter, String source, boolean editable, boolean enabled) {}
}

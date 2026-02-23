package com.example.aitemplate.app;

import com.example.aitemplate.core.model.ModelAdapter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ModelRegistry {

    private final Map<String, ModelAdapter> models;

    public ModelRegistry(List<ModelAdapter> adapters) {
        this.models = adapters.stream().collect(Collectors.toUnmodifiableMap(ModelAdapter::modelId, Function.identity()));
    }

    public List<ModelAdapter> list() {
        return models.values().stream().sorted((a, b) -> a.modelId().compareToIgnoreCase(b.modelId())).toList();
    }

    public ModelAdapter getOrThrow(String modelId) {
        ModelAdapter adapter = models.get(modelId);
        if (adapter == null) {
            throw new IllegalArgumentException("Unknown modelId: " + modelId);
        }
        return adapter;
    }
}

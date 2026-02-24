package com.example.aitemplate.core.model;

public record ModelConfig(
        String modelId,
        String provider,
        String displayName,
        String baseUrl,
        String apiKey,
        String modelName,
        boolean enabled,
        CapabilitySet capabilities,
        int sortOrder) {

    public static ModelConfig openAiCompatible(
            String modelId, String displayName, String baseUrl,
            String apiKey, String modelName) {
        return new ModelConfig(
                modelId, "openai", displayName, baseUrl, apiKey, modelName,
                true, new CapabilitySet(true, true, true, false), 100);
    }
}

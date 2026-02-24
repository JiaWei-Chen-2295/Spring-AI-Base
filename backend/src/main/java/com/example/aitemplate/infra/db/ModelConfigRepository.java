package com.example.aitemplate.infra.db;

import com.example.aitemplate.core.model.CapabilitySet;
import com.example.aitemplate.core.model.ModelConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ModelConfigRepository {

    private static final Logger log = LoggerFactory.getLogger(ModelConfigRepository.class);

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public ModelConfigRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initSchema() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS model_config (
                    model_id     VARCHAR(255) PRIMARY KEY,
                    provider     VARCHAR(100),
                    display_name VARCHAR(255),
                    base_url     VARCHAR(1000),
                    api_key      VARCHAR(500),
                    model_name   VARCHAR(255),
                    enabled      BOOLEAN NOT NULL DEFAULT TRUE,
                    capabilities VARCHAR(500),
                    sort_order   INT NOT NULL DEFAULT 100
                )
                """);
    }

    public void save(ModelConfig config) {
        String capJson = serializeCapabilities(config.capabilities());
        int updated = jdbc.update(
                """
                UPDATE model_config SET provider=?, display_name=?, base_url=?, api_key=?,
                    model_name=?, enabled=?, capabilities=?, sort_order=?
                WHERE model_id=?
                """,
                config.provider(), config.displayName(), config.baseUrl(), config.apiKey(),
                config.modelName(), config.enabled(), capJson, config.sortOrder(),
                config.modelId());
        if (updated == 0) {
            jdbc.update(
                    """
                    INSERT INTO model_config
                        (model_id, provider, display_name, base_url, api_key, model_name, enabled, capabilities, sort_order)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    config.modelId(), config.provider(), config.displayName(), config.baseUrl(),
                    config.apiKey(), config.modelName(), config.enabled(), capJson, config.sortOrder());
        }
    }

    public List<ModelConfig> findAll() {
        return jdbc.query(
                "SELECT * FROM model_config ORDER BY sort_order, model_id",
                (rs, rowNum) -> new ModelConfig(
                        rs.getString("model_id"),
                        rs.getString("provider"),
                        rs.getString("display_name"),
                        rs.getString("base_url"),
                        rs.getString("api_key"),
                        rs.getString("model_name"),
                        rs.getBoolean("enabled"),
                        deserializeCapabilities(rs.getString("capabilities")),
                        rs.getInt("sort_order")));
    }

    public Optional<ModelConfig> findById(String modelId) {
        List<ModelConfig> rows = jdbc.query(
                "SELECT * FROM model_config WHERE model_id = ?",
                (rs, rowNum) -> new ModelConfig(
                        rs.getString("model_id"),
                        rs.getString("provider"),
                        rs.getString("display_name"),
                        rs.getString("base_url"),
                        rs.getString("api_key"),
                        rs.getString("model_name"),
                        rs.getBoolean("enabled"),
                        deserializeCapabilities(rs.getString("capabilities")),
                        rs.getInt("sort_order")),
                modelId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public boolean delete(String modelId) {
        return jdbc.update("DELETE FROM model_config WHERE model_id = ?", modelId) > 0;
    }

    private String serializeCapabilities(CapabilitySet caps) {
        if (caps == null) return null;
        try {
            return objectMapper.writeValueAsString(caps);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize capabilities", e);
            return null;
        }
    }

    private CapabilitySet deserializeCapabilities(String json) {
        if (json == null || json.isBlank()) {
            return new CapabilitySet(true, false, false, false);
        }
        try {
            return objectMapper.readValue(json, CapabilitySet.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize capabilities: {}", json, e);
            return new CapabilitySet(true, false, false, false);
        }
    }
}

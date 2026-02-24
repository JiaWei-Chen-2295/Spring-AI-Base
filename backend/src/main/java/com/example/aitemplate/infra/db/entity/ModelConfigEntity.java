package com.example.aitemplate.infra.db.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.aitemplate.core.model.CapabilitySet;
import com.example.aitemplate.core.model.ModelConfig;
import com.example.aitemplate.infra.db.typehandler.CapabilitySetTypeHandler;

@TableName(value = "model_config", autoResultMap = true)
public class ModelConfigEntity {

    @TableId(value = "model_id", type = IdType.INPUT)
    private String modelId;

    @TableField("provider")
    private String provider;

    @TableField("display_name")
    private String displayName;

    @TableField("base_url")
    private String baseUrl;

    @TableField("api_key")
    private String apiKey;

    @TableField("model_name")
    private String modelName;

    @TableField("enabled")
    private boolean enabled;

    @TableField(value = "capabilities", typeHandler = CapabilitySetTypeHandler.class)
    private CapabilitySet capabilities;

    @TableField("sort_order")
    private int sortOrder;

    public ModelConfigEntity() {}

    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public CapabilitySet getCapabilities() { return capabilities; }
    public void setCapabilities(CapabilitySet capabilities) { this.capabilities = capabilities; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public ModelConfig toDomain() {
        return new ModelConfig(modelId, provider, displayName, baseUrl,
                apiKey, modelName, enabled, capabilities, sortOrder);
    }

    public static ModelConfigEntity fromDomain(ModelConfig config) {
        ModelConfigEntity entity = new ModelConfigEntity();
        entity.modelId = config.modelId();
        entity.provider = config.provider();
        entity.displayName = config.displayName();
        entity.baseUrl = config.baseUrl();
        entity.apiKey = config.apiKey();
        entity.modelName = config.modelName();
        entity.enabled = config.enabled();
        entity.capabilities = config.capabilities();
        entity.sortOrder = config.sortOrder();
        return entity;
    }
}

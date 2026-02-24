package com.example.aitemplate.api.dto;

import com.example.aitemplate.core.model.CapabilitySet;
import com.example.aitemplate.core.model.HealthStatus;

public record ModelAdminInfo(
        String modelId,
        String provider,
        String displayName,
        boolean enabled,
        String source,
        boolean editable,
        CapabilitySet capabilities,
        HealthStatus health) {}

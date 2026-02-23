package com.example.aitemplate.api.dto;

import com.example.aitemplate.core.model.CapabilitySet;
import com.example.aitemplate.core.model.HealthStatus;

public record ModelInfo(String provider, String modelId, CapabilitySet capabilities, HealthStatus health) {}

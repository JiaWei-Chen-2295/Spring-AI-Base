package com.example.aitemplate.core.model;

public record CapabilitySet(boolean chat, boolean tools, boolean jsonMode, boolean vision) {

    public static CapabilitySet chatOnly() {
        return new CapabilitySet(true, false, false, false);
    }
}

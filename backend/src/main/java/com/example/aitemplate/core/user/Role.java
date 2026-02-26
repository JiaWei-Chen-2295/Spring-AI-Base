package com.example.aitemplate.core.user;

public record Role(
        Long id,
        String roleCode,
        String roleName,
        String description,
        Integer status) {

    public boolean isEnabled() {
        return status != null && status == 1;
    }
}

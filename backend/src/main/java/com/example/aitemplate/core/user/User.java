package com.example.aitemplate.core.user;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public record User(
        Long id,
        String username,
        String password,
        String name,
        Integer gender,
        LocalDate birthday,
        String phone,
        String email,
        String address,
        String avatar,
        Integer status,
        Set<Role> roles,
        LocalDateTime createTime,
        LocalDateTime updateTime) {

    public boolean isEnabled() {
        return status != null && status == 1;
    }

    public boolean hasRole(String roleCode) {
        return roles != null && roles.stream()
                .anyMatch(role -> role.roleCode().equals(roleCode));
    }
}

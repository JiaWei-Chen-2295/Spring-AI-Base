package com.example.aitemplate.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

@Schema(description = "分配角色请求")
public record AssignRolesRequest(
        @Schema(description = "角色ID列表")
        Set<Long> roleIds) {}

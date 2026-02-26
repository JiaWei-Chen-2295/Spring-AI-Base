package com.example.aitemplate.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "角色创建/更新请求")
public record RoleUpsertRequest(
        @Schema(description = "角色编码(创建时必填)")
        @NotBlank(message = "角色编码不能为空")
        String roleCode,
        
        @Schema(description = "角色名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "角色名称不能为空")
        String roleName,
        
        @Schema(description = "描述")
        String description,
        
        @Schema(description = "状态 1启用 0禁用")
        Integer status) {}

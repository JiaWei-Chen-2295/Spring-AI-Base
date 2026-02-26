package com.example.aitemplate.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "角色信息")
public record RoleInfo(
        @Schema(description = "角色ID")
        Long id,
        
        @Schema(description = "角色编码")
        String roleCode,
        
        @Schema(description = "角色名称")
        String roleName,
        
        @Schema(description = "描述")
        String description,
        
        @Schema(description = "状态 1启用 0禁用")
        Integer status) {}

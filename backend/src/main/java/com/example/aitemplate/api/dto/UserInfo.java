package com.example.aitemplate.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Schema(description = "用户信息")
public record UserInfo(
        @Schema(description = "用户ID")
        Long id,
        
        @Schema(description = "用户名")
        String username,
        
        @Schema(description = "姓名")
        String name,
        
        @Schema(description = "性别 1男 2女")
        Integer gender,
        
        @Schema(description = "出生日期")
        LocalDate birthday,
        
        @Schema(description = "手机号")
        String phone,
        
        @Schema(description = "邮箱")
        String email,
        
        @Schema(description = "地址")
        String address,
        
        @Schema(description = "头像URL")
        String avatar,
        
        @Schema(description = "状态 1启用 0禁用")
        Integer status,
        
        @Schema(description = "角色列表")
        Set<RoleInfo> roles,
        
        @Schema(description = "创建时间")
        LocalDateTime createTime,
        
        @Schema(description = "更新时间")
        LocalDateTime updateTime) {}

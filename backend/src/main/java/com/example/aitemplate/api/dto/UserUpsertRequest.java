package com.example.aitemplate.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "用户创建/更新请求")
public record UserUpsertRequest(
        @Schema(description = "用户名(创建时必填)")
        @Size(min = 3, max = 50, message = "用户名长度必须在3-50之间")
        String username,
        
        @Schema(description = "密码(创建时必填)")
        @Size(min = 6, max = 100, message = "密码长度必须在6-100之间")
        String password,
        
        @Schema(description = "姓名", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "姓名不能为空")
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
        Integer status) {}

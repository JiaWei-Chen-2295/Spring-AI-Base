package com.example.aitemplate.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "修改密码请求")
public record ChangePasswordRequest(
        @Schema(description = "旧密码", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "旧密码不能为空")
        String oldPassword,
        
        @Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "新密码不能为空")
        String newPassword) {}

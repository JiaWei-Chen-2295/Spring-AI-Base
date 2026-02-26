package com.example.aitemplate.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;

@Schema(description = "更新用户资料请求")
public record UpdateUserRequest(
        @Schema(description = "用户姓名")
        String name,
        
        @Schema(description = "性别：0-未知，1-男，2-女")
        Integer gender,
        
        @Schema(description = "手机号")
        String phone,
        
        @Schema(description = "邮箱")
        @Email(message = "邮箱格式不正确")
        String email,
        
        @Schema(description = "头像URL")
        String avatar) {}

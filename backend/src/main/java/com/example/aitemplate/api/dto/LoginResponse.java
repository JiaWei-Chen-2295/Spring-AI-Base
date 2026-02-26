package com.example.aitemplate.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登录响应")
public record LoginResponse(
        @Schema(description = "访问令牌")
        String accessToken,
        
        @Schema(description = "刷新令牌")
        String refreshToken,
        
        @Schema(description = "令牌类型", example = "Bearer")
        String tokenType,
        
        @Schema(description = "过期时间(秒)", example = "7200")
        Long expiresIn,
        
        @Schema(description = "用户信息")
        UserInfo user) {}

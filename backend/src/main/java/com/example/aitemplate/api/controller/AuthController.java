package com.example.aitemplate.api.controller;

import com.example.aitemplate.core.PublicApi;
import com.example.aitemplate.api.dto.ChangePasswordRequest;
import com.example.aitemplate.api.dto.LoginRequest;
import com.example.aitemplate.api.dto.LoginResponse;
import com.example.aitemplate.api.dto.UpdateUserRequest;
import com.example.aitemplate.api.dto.UserInfo;
import com.example.aitemplate.core.user.User;
import com.example.aitemplate.infra.db.UserRepository;
import com.example.aitemplate.infra.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证管理", description = "用户登录、登出、Token刷新")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider tokenProvider,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Operation(summary = "用户登录", description = "通过用户名密码获取Token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误")
    })
    @PublicApi
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(request.username());
        
        User user = userRepository.findByUsername(request.username()).orElseThrow();
        
        LoginResponse response = new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                tokenProvider.getAccessTokenExpiration(),
                toUserInfo(user)
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "用户登出", description = "登出当前用户（客户端需清除Token）")
    @PublicApi
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "刷新Token", description = "使用RefreshToken获取新的AccessToken")
    @PublicApi
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(401).build();
        }
        
        String username = tokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username).orElseThrow();
        
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password(user.password())
                .roles(user.roles().stream().map(r -> r.roleCode()).toArray(String[]::new))
                .build();
        
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        
        String newAccessToken = tokenProvider.generateAccessToken(authentication);
        String newRefreshToken = tokenProvider.generateRefreshToken(username);
        
        LoginResponse response = new LoginResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                tokenProvider.getAccessTokenExpiration(),
                toUserInfo(user)
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    @GetMapping("/me")
    public ResponseEntity<UserInfo> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        return ResponseEntity.ok(toUserInfo(user));
    }

    @Operation(summary = "修改密码", description = "修改当前用户密码")
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        
        if (!passwordEncoder.matches(request.oldPassword(), user.password())) {
            return ResponseEntity.status(400).build();
        }
        
        User updatedUser = new User(
                user.id(),
                user.username(),
                passwordEncoder.encode(request.newPassword()),
                user.name(),
                user.gender(),
                user.birthday(),
                user.phone(),
                user.email(),
                user.address(),
                user.avatar(),
                user.status(),
                user.roles(),
                user.createTime(),
                user.updateTime()
        );
        userRepository.save(updatedUser);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "更新用户资料", description = "更新当前登录用户的个人资料")
    @PutMapping("/profile")
    public ResponseEntity<UserInfo> updateProfile(@Valid @RequestBody UpdateUserRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        
        User updatedUser = new User(
                user.id(),
                user.username(),
                user.password(),
                request.name() != null ? request.name() : user.name(),
                request.gender() != null ? request.gender() : user.gender(),
                user.birthday(),
                request.phone() != null ? request.phone() : user.phone(),
                request.email() != null ? request.email() : user.email(),
                user.address(),
                request.avatar() != null ? request.avatar() : user.avatar(),
                user.status(),
                user.roles(),
                user.createTime(),
                java.time.LocalDateTime.now()
        );
        userRepository.save(updatedUser);
        return ResponseEntity.ok(toUserInfo(updatedUser));
    }

    private UserInfo toUserInfo(User user) {
        return new UserInfo(
                user.id(),
                user.username(),
                user.name(),
                user.gender(),
                user.birthday(),
                user.phone(),
                user.email(),
                user.address(),
                user.avatar(),
                user.status(),
                user.roles().stream()
                        .map(r -> new com.example.aitemplate.api.dto.RoleInfo(
                                r.id(), r.roleCode(), r.roleName(), r.description(), r.status()))
                        .collect(java.util.stream.Collectors.toSet()),
                user.createTime(),
                user.updateTime()
        );
    }
}

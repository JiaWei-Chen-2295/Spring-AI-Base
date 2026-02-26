package com.example.aitemplate.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aitemplate.api.dto.AssignRolesRequest;
import com.example.aitemplate.api.dto.UserInfo;
import com.example.aitemplate.api.dto.UserUpsertRequest;
import com.example.aitemplate.core.user.Role;
import com.example.aitemplate.core.user.User;
import com.example.aitemplate.infra.db.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理", description = "用户的增删改查、角色分配")
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Operation(summary = "用户列表", description = "分页查询用户列表，支持关键字搜索")
    @GetMapping
    public ResponseEntity<Page<UserInfo>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        Page<User> userPage = userRepository.findAll(pageNum, pageSize, keyword);
        Page<UserInfo> infoPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        infoPage.setRecords(userPage.getRecords().stream()
                .map(this::toUserInfo)
                .toList());
        return ResponseEntity.ok(infoPage);
    }

    @Operation(summary = "用户详情", description = "根据ID获取用户详细信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserInfo> get(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(user -> ResponseEntity.ok(toUserInfo(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "创建用户", description = "创建新用户")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "用户名已存在")
    })
    @PostMapping
    public ResponseEntity<UserInfo> create(@Valid @RequestBody UserUpsertRequest request) {
        if (request.username() == null || request.username().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (userRepository.existsByUsername(request.username())) {
            return ResponseEntity.badRequest().build();
        }
        
        User user = new User(
                null,
                request.username(),
                passwordEncoder.encode(request.password() != null ? request.password() : "123456"),
                request.name(),
                request.gender(),
                request.birthday(),
                request.phone(),
                request.email(),
                request.address(),
                request.avatar(),
                request.status() != null ? request.status() : 1,
                Set.of(),
                null,
                null
        );
        User saved = userRepository.save(user);
        return ResponseEntity.ok(toUserInfo(saved));
    }

    @Operation(summary = "更新用户", description = "更新用户信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserInfo> update(@PathVariable Long id, @Valid @RequestBody UserUpsertRequest request) {
        return userRepository.findById(id)
                .map(existing -> {
                    User user = new User(
                            existing.id(),
                            existing.username(),
                            request.password() != null && !request.password().isBlank()
                                    ? passwordEncoder.encode(request.password())
                                    : existing.password(),
                            request.name(),
                            request.gender(),
                            request.birthday(),
                            request.phone(),
                            request.email(),
                            request.address(),
                            request.avatar(),
                            request.status(),
                            existing.roles(),
                            existing.createTime(),
                            existing.updateTime()
                    );
                    User saved = userRepository.save(user);
                    return ResponseEntity.ok(toUserInfo(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "删除用户", description = "逻辑删除用户")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (userRepository.delete(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "启用/禁用用户", description = "切换用户状态")
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserInfo> toggleStatus(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(existing -> {
                    User user = new User(
                            existing.id(),
                            existing.username(),
                            existing.password(),
                            existing.name(),
                            existing.gender(),
                            existing.birthday(),
                            existing.phone(),
                            existing.email(),
                            existing.address(),
                            existing.avatar(),
                            existing.status() == 1 ? 0 : 1,
                            existing.roles(),
                            existing.createTime(),
                            existing.updateTime()
                    );
                    User saved = userRepository.save(user);
                    return ResponseEntity.ok(toUserInfo(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "分配角色", description = "为用户分配角色")
    @PostMapping("/{id}/roles")
    public ResponseEntity<Void> assignRoles(@PathVariable Long id, @RequestBody AssignRolesRequest request) {
        if (userRepository.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        userRepository.assignRoles(id, request.roleIds());
        return ResponseEntity.ok().build();
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
                        .collect(Collectors.toSet()),
                user.createTime(),
                user.updateTime()
        );
    }
}

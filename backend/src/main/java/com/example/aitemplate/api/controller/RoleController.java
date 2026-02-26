package com.example.aitemplate.api.controller;

import com.example.aitemplate.api.dto.RoleInfo;
import com.example.aitemplate.api.dto.RoleUpsertRequest;
import com.example.aitemplate.core.user.Role;
import com.example.aitemplate.infra.db.RoleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "角色管理", description = "角色的增删改查")
@RestController
@RequestMapping("/api/admin/roles")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleRepository roleRepository;

    public RoleController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Operation(summary = "角色列表", description = "获取所有角色")
    @GetMapping
    public List<RoleInfo> list() {
        return roleRepository.findAll().stream()
                .map(this::toRoleInfo)
                .toList();
    }

    @Operation(summary = "角色详情", description = "根据ID获取角色详细信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "角色不存在")
    })
    @GetMapping("/{id}")
    public ResponseEntity<RoleInfo> get(@PathVariable Long id) {
        return roleRepository.findById(id)
                .map(role -> ResponseEntity.ok(toRoleInfo(role)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "创建角色", description = "创建新角色")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "角色编码已存在")
    })
    @PostMapping
    public ResponseEntity<RoleInfo> create(@Valid @RequestBody RoleUpsertRequest request) {
        if (roleRepository.existsByRoleCode(request.roleCode())) {
            return ResponseEntity.badRequest().build();
        }
        
        Role role = new Role(
                null,
                request.roleCode(),
                request.roleName(),
                request.description(),
                request.status() != null ? request.status() : 1
        );
        Role saved = roleRepository.save(role);
        return ResponseEntity.ok(toRoleInfo(saved));
    }

    @Operation(summary = "更新角色", description = "更新角色信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "角色不存在")
    })
    @PutMapping("/{id}")
    public ResponseEntity<RoleInfo> update(@PathVariable Long id, @Valid @RequestBody RoleUpsertRequest request) {
        return roleRepository.findById(id)
                .map(existing -> {
                    Role role = new Role(
                            existing.id(),
                            existing.roleCode(),
                            request.roleName(),
                            request.description(),
                            request.status()
                    );
                    Role saved = roleRepository.save(role);
                    return ResponseEntity.ok(toRoleInfo(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "删除角色", description = "逻辑删除角色")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (roleRepository.delete(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    private RoleInfo toRoleInfo(Role role) {
        return new RoleInfo(
                role.id(),
                role.roleCode(),
                role.roleName(),
                role.description(),
                role.status()
        );
    }
}

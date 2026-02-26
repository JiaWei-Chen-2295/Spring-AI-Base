package com.example.aitemplate.infra.db;

import com.example.aitemplate.core.user.Role;
import com.example.aitemplate.core.user.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 查找ADMIN角色
        Role adminRole = roleRepository.findByRoleCode("ADMIN").orElse(null);
        if (adminRole == null) {
            return;
        }

        // 检查是否已存在admin用户
        var existingAdmin = userRepository.findByUsername("admin");
        if (existingAdmin.isPresent()) {
            User admin = existingAdmin.get();
            // 确保admin有ADMIN角色
            if (admin.roles() == null || admin.roles().isEmpty()) {
                userRepository.assignRoles(admin.id(), Set.of(adminRole.id()));
            }
            return;
        }

        // 创建管理员用户
        User admin = new User(
                null,
                "admin",
                passwordEncoder.encode("admin123"),
                "系统管理员",
                1,
                null,
                null,
                "admin@example.com",
                null,
                null,
                1,
                Set.of(),
                null,
                null
        );
        User savedAdmin = userRepository.save(admin);
        
        // 分配ADMIN角色
        userRepository.assignRoles(savedAdmin.id(), Set.of(adminRole.id()));
    }
}

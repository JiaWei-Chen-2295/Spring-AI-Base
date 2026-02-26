package com.example.aitemplate.infra.db;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aitemplate.core.user.Role;
import com.example.aitemplate.infra.db.entity.RoleEntity;
import com.example.aitemplate.infra.db.mapper.RoleMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class RoleRepository {

    private final RoleMapper roleMapper;

    public RoleRepository(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    public List<Role> findAll() {
        return roleMapper.selectList(null).stream()
                .map(RoleEntity::toDomain)
                .toList();
    }

    public Optional<Role> findById(Long id) {
        RoleEntity entity = roleMapper.selectById(id);
        return Optional.ofNullable(entity).map(RoleEntity::toDomain);
    }

    public Optional<Role> findByRoleCode(String roleCode) {
        LambdaQueryWrapper<RoleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleEntity::getRoleCode, roleCode);
        RoleEntity entity = roleMapper.selectOne(wrapper);
        return Optional.ofNullable(entity).map(RoleEntity::toDomain);
    }

    public Role save(Role role) {
        RoleEntity entity = RoleEntity.fromDomain(role);
        if (role.id() == null) {
            roleMapper.insert(entity);
        } else {
            roleMapper.updateById(entity);
        }
        return entity.toDomain();
    }

    public boolean delete(Long id) {
        return roleMapper.deleteById(id) > 0;
    }

    public boolean existsByRoleCode(String roleCode) {
        LambdaQueryWrapper<RoleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleEntity::getRoleCode, roleCode);
        return roleMapper.selectCount(wrapper) > 0;
    }
}

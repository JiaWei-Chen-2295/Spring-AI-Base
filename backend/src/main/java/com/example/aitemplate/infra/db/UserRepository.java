package com.example.aitemplate.infra.db;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aitemplate.core.user.Role;
import com.example.aitemplate.core.user.User;
import com.example.aitemplate.infra.db.entity.RoleEntity;
import com.example.aitemplate.infra.db.entity.UserEntity;
import com.example.aitemplate.infra.db.entity.UserRoleEntity;
import com.example.aitemplate.infra.db.mapper.RoleMapper;
import com.example.aitemplate.infra.db.mapper.UserMapper;
import com.example.aitemplate.infra.db.mapper.UserRoleMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class UserRepository {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public UserRepository(UserMapper userMapper, RoleMapper roleMapper, UserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    public Optional<User> findById(Long id) {
        UserEntity entity = userMapper.selectById(id);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(toDomainWithRoles(entity));
    }

    public Optional<User> findByUsername(String username) {
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserEntity::getUsername, username);
        UserEntity entity = userMapper.selectOne(wrapper);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(toDomainWithRoles(entity));
    }

    public Page<User> findAll(int pageNum, int pageSize, String keyword) {
        Page<UserEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(UserEntity::getName, keyword)
                    .or().like(UserEntity::getUsername, keyword)
                    .or().like(UserEntity::getPhone, keyword)
                    .or().like(UserEntity::getEmail, keyword);
        }
        wrapper.orderByDesc(UserEntity::getCreateTime);
        
        Page<UserEntity> entityPage = userMapper.selectPage(page, wrapper);
        
        Page<User> userPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        userPage.setRecords(entityPage.getRecords().stream()
                .map(this::toDomainWithRoles)
                .toList());
        return userPage;
    }

    public List<User> findAll() {
        return userMapper.selectList(null).stream()
                .map(this::toDomainWithRoles)
                .toList();
    }

    @Transactional
    public User save(User user) {
        UserEntity entity = toEntity(user);
        if (user.id() == null) {
            userMapper.insert(entity);
        } else {
            userMapper.updateById(entity);
        }
        return toDomainWithRoles(entity);
    }

    @Transactional
    public void assignRoles(Long userId, Set<Long> roleIds) {
        userRoleMapper.deleteByUserId(userId);
        for (Long roleId : roleIds) {
            UserRoleEntity ur = new UserRoleEntity();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
    }

    public boolean delete(Long id) {
        return userMapper.deleteById(id) > 0;
    }

    public boolean existsByUsername(String username) {
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserEntity::getUsername, username);
        return userMapper.selectCount(wrapper) > 0;
    }

    private User toDomainWithRoles(UserEntity entity) {
        List<Long> roleIds = userMapper.findRoleIdsByUserId(entity.getId());
        Set<Role> roles = Set.of();
        if (!roleIds.isEmpty()) {
            roles = roleMapper.selectBatchIds(roleIds).stream()
                    .map(RoleEntity::toDomain)
                    .collect(Collectors.toSet());
        }
        return new User(
                entity.getId(),
                entity.getUsername(),
                entity.getPassword(),
                entity.getName(),
                entity.getGender(),
                entity.getBirthday(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getAddress(),
                entity.getAvatar(),
                entity.getStatus(),
                roles,
                entity.getCreateTime(),
                entity.getUpdateTime()
        );
    }

    private UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.id());
        entity.setUsername(user.username());
        entity.setPassword(user.password());
        entity.setName(user.name());
        entity.setGender(user.gender());
        entity.setBirthday(user.birthday());
        entity.setPhone(user.phone());
        entity.setEmail(user.email());
        entity.setAddress(user.address());
        entity.setAvatar(user.avatar());
        entity.setStatus(user.status());
        return entity;
    }
}

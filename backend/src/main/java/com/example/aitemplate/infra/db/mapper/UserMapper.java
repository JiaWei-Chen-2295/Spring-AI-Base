package com.example.aitemplate.infra.db.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aitemplate.infra.db.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

    @Select("SELECT ur.role_id FROM user_role ur " +
            "INNER JOIN role r ON ur.role_id = r.id " +
            "WHERE ur.user_id = #{userId} AND r.deleted = 0")
    List<Long> findRoleIdsByUserId(@Param("userId") Long userId);
}

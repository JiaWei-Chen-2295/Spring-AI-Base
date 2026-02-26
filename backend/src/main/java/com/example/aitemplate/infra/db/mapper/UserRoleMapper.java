package com.example.aitemplate.infra.db.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aitemplate.infra.db.entity.UserRoleEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRoleEntity> {

    @Delete("DELETE FROM user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}

package com.example.aitemplate.infra.db.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aitemplate.infra.db.entity.AppSettingEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppSettingMapper extends BaseMapper<AppSettingEntity> {
}

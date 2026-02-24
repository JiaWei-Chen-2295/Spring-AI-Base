package com.example.aitemplate.infra.db;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aitemplate.core.model.SettingEntry;
import com.example.aitemplate.infra.db.entity.AppSettingEntity;
import com.example.aitemplate.infra.db.mapper.AppSettingMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class AppSettingsRepository {

    private final AppSettingMapper mapper;

    public AppSettingsRepository(AppSettingMapper mapper) {
        this.mapper = mapper;
    }

    public String getValue(String key) {
        AppSettingEntity entity = mapper.selectById(key);
        return entity == null ? null : entity.getSettingValue();
    }

    public void setValue(String key, String value, String description) {
        AppSettingEntity entity = new AppSettingEntity(key, value, description);
        if (mapper.selectById(key) != null) {
            mapper.updateById(entity);
        } else {
            mapper.insert(entity);
        }
    }

    public List<SettingEntry> findAll() {
        LambdaQueryWrapper<AppSettingEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(AppSettingEntity::getSettingKey);
        return mapper.selectList(wrapper).stream()
                .map(AppSettingEntity::toDomain)
                .toList();
    }
}

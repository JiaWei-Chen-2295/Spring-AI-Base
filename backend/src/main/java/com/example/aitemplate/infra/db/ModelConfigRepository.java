package com.example.aitemplate.infra.db;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aitemplate.core.model.ModelConfig;
import com.example.aitemplate.infra.db.entity.ModelConfigEntity;
import com.example.aitemplate.infra.db.mapper.ModelConfigMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ModelConfigRepository {

    private final ModelConfigMapper mapper;

    public ModelConfigRepository(ModelConfigMapper mapper) {
        this.mapper = mapper;
    }

    public void save(ModelConfig config) {
        ModelConfigEntity entity = ModelConfigEntity.fromDomain(config);
        if (mapper.selectById(config.modelId()) != null) {
            mapper.updateById(entity);
        } else {
            mapper.insert(entity);
        }
    }

    public List<ModelConfig> findAll() {
        LambdaQueryWrapper<ModelConfigEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(ModelConfigEntity::getSortOrder, ModelConfigEntity::getModelId);
        return mapper.selectList(wrapper).stream()
                .map(ModelConfigEntity::toDomain)
                .toList();
    }

    public Optional<ModelConfig> findById(String modelId) {
        ModelConfigEntity entity = mapper.selectById(modelId);
        return Optional.ofNullable(entity).map(ModelConfigEntity::toDomain);
    }

    public boolean delete(String modelId) {
        return mapper.deleteById(modelId) > 0;
    }
}

package com.example.aitemplate.infra.db.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.aitemplate.core.model.SettingEntry;

@TableName("app_settings")
public class AppSettingEntity {

    @TableId(value = "setting_key", type = IdType.INPUT)
    private String settingKey;

    @TableField("setting_value")
    private String settingValue;

    @TableField("description")
    private String description;

    public AppSettingEntity() {}

    public AppSettingEntity(String settingKey, String settingValue, String description) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.description = description;
    }

    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }

    public String getSettingValue() { return settingValue; }
    public void setSettingValue(String settingValue) { this.settingValue = settingValue; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public SettingEntry toDomain() {
        return new SettingEntry(settingKey, settingValue, description);
    }
}

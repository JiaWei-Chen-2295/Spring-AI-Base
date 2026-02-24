package com.example.aitemplate.app;

import com.example.aitemplate.infra.db.AppSettingsRepository;
import com.example.aitemplate.core.model.SettingEntry;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {

    public static final String KEY_DASHSCOPE_API_KEY = "provider.dashscope.api-key";
    public static final String KEY_OPENAI_API_KEY = "provider.openai.api-key";

    private final AppSettingsRepository settingsRepo;
    private final String envDashScopeKey;
    private final String envOpenAiKey;

    public SettingsService(
            AppSettingsRepository settingsRepo,
            @Value("${DASHSCOPE_API_KEY:}") String envDashScopeKey,
            @Value("${OPENAI_API_KEY:}") String envOpenAiKey) {
        this.settingsRepo = settingsRepo;
        this.envDashScopeKey = envDashScopeKey;
        this.envOpenAiKey = envOpenAiKey;
    }

    /** Returns the effective DashScope API key: DB config first, then env var fallback. */
    public String getDashScopeApiKey() {
        String dbValue = settingsRepo.getValue(KEY_DASHSCOPE_API_KEY);
        if (dbValue != null && !dbValue.isBlank()) return dbValue;
        return envDashScopeKey;
    }

    public void setDashScopeApiKey(String apiKey) {
        settingsRepo.setValue(KEY_DASHSCOPE_API_KEY, apiKey, "DashScope (通义千问) API Key");
    }

    /** Returns the effective OpenAI API key: DB config first, then env var fallback. */
    public String getOpenAiApiKey() {
        String dbValue = settingsRepo.getValue(KEY_OPENAI_API_KEY);
        if (dbValue != null && !dbValue.isBlank()) return dbValue;
        return envOpenAiKey;
    }

    public void setOpenAiApiKey(String apiKey) {
        settingsRepo.setValue(KEY_OPENAI_API_KEY, apiKey, "OpenAI API Key");
    }

    public void set(String key, String value) {
        switch (key) {
            case KEY_DASHSCOPE_API_KEY -> setDashScopeApiKey(value);
            case KEY_OPENAI_API_KEY -> setOpenAiApiKey(value);
            default -> settingsRepo.setValue(key, value, null);
        }
    }

    public List<SettingEntry> findAll() {
        return settingsRepo.findAll();
    }
}

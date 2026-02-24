package com.example.aitemplate.api.controller;

import com.example.aitemplate.app.SettingsService;
import com.example.aitemplate.infra.db.AppSettingsRepository.SettingEntry;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/settings")
public class SettingsAdminController {

    private final SettingsService settingsService;

    public SettingsAdminController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /** List all settings. API key values are masked in the response. */
    @GetMapping
    public List<SettingDto> listSettings() {
        List<SettingEntry> entries = settingsService.findAll();

        // Include known keys even if not yet persisted in DB
        List<String> knownKeys = List.of(
                SettingsService.KEY_DASHSCOPE_API_KEY,
                SettingsService.KEY_OPENAI_API_KEY);

        List<SettingDto> result = new java.util.ArrayList<>();
        for (String key : knownKeys) {
            SettingEntry entry = entries.stream()
                    .filter(e -> e.key().equals(key))
                    .findFirst()
                    .orElse(null);
            String rawValue = (entry != null) ? entry.value() : null;
            boolean configured = rawValue != null && !rawValue.isBlank();
            String maskedValue = configured ? "***" : "";
            String description = (entry != null && entry.description() != null)
                    ? entry.description()
                    : defaultDescription(key);
            result.add(new SettingDto(key, maskedValue, description, configured));
        }
        return result;
    }

    /** Update a setting value. */
    @PutMapping("/{key}")
    public ResponseEntity<SettingDto> updateSetting(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        String value = body.get("value");
        if (value == null) {
            return ResponseEntity.badRequest().build();
        }
        settingsService.set(key, value);
        boolean configured = !value.isBlank();
        return ResponseEntity.ok(new SettingDto(key, configured ? "***" : "", defaultDescription(key), configured));
    }

    private String defaultDescription(String key) {
        return switch (key) {
            case SettingsService.KEY_DASHSCOPE_API_KEY -> "DashScope (通义千问) API Key";
            case SettingsService.KEY_OPENAI_API_KEY -> "OpenAI API Key";
            default -> "";
        };
    }

    public record SettingDto(String key, String value, String description, boolean configured) {}
}

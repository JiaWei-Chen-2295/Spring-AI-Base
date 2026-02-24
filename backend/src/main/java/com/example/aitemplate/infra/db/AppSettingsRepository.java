package com.example.aitemplate.infra.db;

import jakarta.annotation.PostConstruct;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AppSettingsRepository {

    private final JdbcTemplate jdbc;

    public AppSettingsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void initSchema() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS app_settings (
                    setting_key   VARCHAR(255) PRIMARY KEY,
                    setting_value VARCHAR(2000),
                    description   VARCHAR(500)
                )
                """);
    }

    public String getValue(String key) {
        List<String> rows = jdbc.query(
                "SELECT setting_value FROM app_settings WHERE setting_key = ?",
                (rs, rowNum) -> rs.getString("setting_value"),
                key);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void setValue(String key, String value, String description) {
        int updated = jdbc.update(
                "UPDATE app_settings SET setting_value = ?, description = ? WHERE setting_key = ?",
                value, description, key);
        if (updated == 0) {
            jdbc.update(
                    "INSERT INTO app_settings (setting_key, setting_value, description) VALUES (?, ?, ?)",
                    key, value, description);
        }
    }

    public List<SettingEntry> findAll() {
        return jdbc.query(
                "SELECT setting_key, setting_value, description FROM app_settings ORDER BY setting_key",
                (rs, rowNum) -> new SettingEntry(
                        rs.getString("setting_key"),
                        rs.getString("setting_value"),
                        rs.getString("description")));
    }

    public record SettingEntry(String key, String value, String description) {}
}

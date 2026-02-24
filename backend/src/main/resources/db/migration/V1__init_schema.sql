CREATE TABLE IF NOT EXISTS app_settings (
    setting_key   VARCHAR(255) PRIMARY KEY,
    setting_value VARCHAR(2000),
    description   VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS model_config (
    model_id     VARCHAR(255) PRIMARY KEY,
    provider     VARCHAR(100),
    display_name VARCHAR(255),
    base_url     VARCHAR(1000),
    api_key      VARCHAR(500),
    model_name   VARCHAR(255),
    enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    capabilities VARCHAR(500),
    sort_order   INT NOT NULL DEFAULT 100
);

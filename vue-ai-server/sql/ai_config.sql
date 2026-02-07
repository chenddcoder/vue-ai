-- AI配置表
CREATE TABLE magic_sys_ai_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    provider_id TEXT NOT NULL,
    model_id TEXT NOT NULL,
    config TEXT NOT NULL,  -- JSON格式存储配置信息
    is_active INTEGER DEFAULT 0,  -- 0: false, 1: true
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES magic_sys_user(id) ON DELETE CASCADE,
    UNIQUE(user_id, provider_id, model_id)  -- 同一用户同一模型只能有一个配置
);

-- 创建索引
CREATE INDEX idx_ai_config_user_id ON magic_sys_ai_config(user_id);
CREATE INDEX idx_ai_config_active ON magic_sys_ai_config(user_id, is_active);

-- 插入一些示例配置（可选）
INSERT INTO magic_sys_ai_config (user_id, provider_id, model_id, config, is_active) 
VALUES (1, 'openai', 'gpt-3.5-turbo', '{"apiKey":"sk-test"}', 1);
package com.vueai.server.service;

import com.vueai.server.model.AIConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class AIConfigService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<AIConfig> aiConfigRowMapper = new RowMapper<AIConfig>() {
        @Override
        public AIConfig mapRow(ResultSet rs, int rowNum) throws SQLException {
            AIConfig config = new AIConfig();
            config.setId(rs.getInt("id"));
            config.setUserId(rs.getInt("user_id"));
            config.setProviderId(rs.getString("provider_id"));
            config.setModelId(rs.getString("model_id"));
            config.setConfig(rs.getString("config"));
            config.setIsActive(rs.getBoolean("is_active"));
            config.setCreateTime(rs.getString("create_time"));
            config.setUpdateTime(rs.getString("update_time"));
            return config;
        }
    };

    // 获取用户的所有AI配置
    public List<AIConfig> getAIConfigsByUserId(Integer userId) {
        String sql = "SELECT * FROM magic_sys_ai_config WHERE user_id = ? ORDER BY is_active DESC, update_time DESC";
        return jdbcTemplate.query(sql, aiConfigRowMapper, userId);
    }

    // 保存AI配置
    @Transactional
    public AIConfig saveAIConfig(AIConfig aiConfig) {
        // 首先检查是否已存在相同配置（根据UNIQUE约束）
        String checkSql = "SELECT id FROM magic_sys_ai_config WHERE user_id = ? AND provider_id = ? AND model_id = ?";

        Integer existingId = null;
        try {
            existingId = jdbcTemplate.queryForObject(checkSql, Integer.class,
                aiConfig.getUserId(), aiConfig.getProviderId(), aiConfig.getModelId());
        } catch (Exception e) {
            // 没有找到记录，这是正常的
        }

        if (existingId != null) {
            // 已存在，更新
            String updateSql = "UPDATE magic_sys_ai_config SET config = ?, is_active = ?, update_time = datetime('now') WHERE id = ?";
            jdbcTemplate.update(updateSql, aiConfig.getConfig(), aiConfig.getIsActive(), existingId);
            aiConfig.setId(existingId);
        } else {
            // 不存在，插入新配置
            String insertSql = "INSERT INTO magic_sys_ai_config (user_id, provider_id, model_id, config, is_active, create_time, update_time) VALUES (?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
            jdbcTemplate.update(insertSql, aiConfig.getUserId(), aiConfig.getProviderId(),
                aiConfig.getModelId(), aiConfig.getConfig(), aiConfig.getIsActive());

            // 获取插入的ID
            Integer insertedId = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Integer.class);
            aiConfig.setId(insertedId);
        }

        return aiConfig;
    }

    // 删除AI配置
    public void deleteAIConfig(Integer id) {
        String sql = "DELETE FROM magic_sys_ai_config WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    // 设置活动AI配置
    @Transactional
    public AIConfig setActiveAIConfig(Integer configId) {
        // 先获取配置信息
        String getSql = "SELECT * FROM magic_sys_ai_config WHERE id = ?";
        AIConfig config = jdbcTemplate.queryForObject(getSql, aiConfigRowMapper, configId);
        
        if (config == null) {
            throw new RuntimeException("AI config not found with id: " + configId);
        }

        // 将该用户的所有配置设为非活动
        String updateAllSql = "UPDATE magic_sys_ai_config SET is_active = 0 WHERE user_id = ?";
        jdbcTemplate.update(updateAllSql, config.getUserId());

        // 设置指定配置为活动
        String updateSql = "UPDATE magic_sys_ai_config SET is_active = 1, update_time = datetime('now') WHERE id = ?";
        jdbcTemplate.update(updateSql, configId);

        config.setIsActive(true);
        return config;
    }

    // 获取活动AI配置
    public AIConfig getActiveAIConfig(Integer userId) {
        String sql = "SELECT * FROM magic_sys_ai_config WHERE user_id = ? AND is_active = 1";
        List<AIConfig> configs = jdbcTemplate.query(sql, aiConfigRowMapper, userId);
        return configs.isEmpty() ? null : configs.get(0);
    }

    // 获取AI提供商信息
    public List<Map<String, Object>> getAIProviders() {
        // 返回支持的AI提供商信息
        return Arrays.asList(
            createProviderMap(
                "openai", "OpenAI", "OpenAI GPT", "openai",
                "OpenAI的GPT系列模型，提供强大的代码生成能力", "🤖", "https://openai.com"
            ),
            createProviderMap(
                "anthropic", "Anthropic", "Claude", "anthropic",
                "Anthropic的Claude系列模型，专注于安全和有用的AI助手", "🧠", "https://anthropic.com"
            ),
            createProviderMap(
                "azure", "Azure OpenAI", "Azure OpenAI", "azure",
                "微软Azure托管的OpenAI服务，提供企业级的稳定性保障", "☁️", "https://azure.microsoft.com/products/ai-services/openai-service"
            ),
            createProviderMap(
                "local", "Local AI", "本地模型", "local",
                "在本地运行的AI模型，如Ollama、LM Studio等", "🏠", null
            ),
            createProviderMap(
                "custom", "Custom", "自定义API", "custom",
                "支持任何兼容OpenAI格式的自定义API", "⚙️", null
            )
        );
    }

    private Map<String, Object> createProviderMap(String id, String name, String displayName, String type, String description, String icon, String website) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("displayName", displayName);
        map.put("type", type);
        map.put("description", description);
        map.put("icon", icon);
        if (website != null) {
            map.put("website", website);
        }
        return map;
    }

    // 获取指定提供商的模型信息
    public List<Map<String, Object>> getAIModels(String providerId) {
        switch (providerId) {
            case "openai":
                return Arrays.asList(
                    createModelMap(
                        "gpt-4", "gpt-4", "GPT-4", 8192, 4096
                    ),
                    createModelMap(
                        "gpt-4-turbo", "gpt-4-turbo", "GPT-4 Turbo", 128000, 4096
                    ),
                    createModelMap(
                        "gpt-3.5-turbo", "gpt-3.5-turbo", "GPT-3.5 Turbo", 16384, 4096
                    )
                );
            case "anthropic":
                return Arrays.asList(
                    createModelMap(
                        "claude-3-opus-20240229", "claude-3-opus-20240229", "Claude 3 Opus", 200000, 4096
                    ),
                    createModelMap(
                        "claude-3-sonnet-20240229", "claude-3-sonnet-20240229", "Claude 3 Sonnet", 200000, 4096
                    ),
                    createModelMap(
                        "claude-3-haiku-20240307", "claude-3-haiku-20240307", "Claude 3 Haiku", 200000, 4096
                    )
                );
            case "azure":
                return Arrays.asList(
                    createModelMap(
                        "gpt-4", "gpt-4", "GPT-4 (Azure)", 8192, 4096
                    ),
                    createModelMap(
                        "gpt-35-turbo", "gpt-35-turbo", "GPT-3.5 Turbo (Azure)", 16384, 4096
                    )
                );
            default:
                return Arrays.asList(
                    createModelMap(
                        "custom", "custom", "自定义模型", 4096, 4096
                    )
                );
        }
    }

    private Map<String, Object> createModelMap(String id, String name, String displayName, int contextWindow, int maxTokens) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("displayName", displayName);
        map.put("contextWindow", contextWindow);
        map.put("maxTokens", maxTokens);
        return map;
    }
}
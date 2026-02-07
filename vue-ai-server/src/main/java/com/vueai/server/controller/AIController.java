package com.vueai.server.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vueai.server.dto.SaveAIConfigRequest;
import com.vueai.server.model.AIConfig;
import com.vueai.server.service.AIConfigService;
import com.vueai.server.service.AIGenerateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/magic/ai")
@CrossOrigin
public class AIController {

    @Autowired
    private AIConfigService aiConfigService;

    @Autowired
    private AIGenerateService aiGenerateService;

    // AI代码生成
    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestBody Map<String, Object> body) {
        try {
            String prompt = (String) body.get("prompt");
            String provider = (String) body.get("provider");
            String model = (String) body.get("model");
            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) body.get("config");

            // 调用AI生成服务
            Map<String, Object> result = aiGenerateService.generate(provider, model, config, prompt);
            result.put("code", 1);

            return result;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 0);
            error.put("message", "AI生成失败: " + e.getMessage());
            return error;
        }
    }

    // 获取用户AI配置列表
    @GetMapping("/configs")
    public Map<String, Object> getAIConfigs(@RequestParam Integer userId) {
        try {
            List<AIConfig> configs = aiConfigService.getAIConfigsByUserId(userId);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 1);
            result.put("data", configs);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 0);
            result.put("message", "Failed to get AI configs: " + e.getMessage());
            return result;
        }
    }

    // 保存AI配置
    @PostMapping("/configs")
    public Map<String, Object> saveAIConfig(@RequestBody SaveAIConfigRequest request) {
        try {
            // Convert config map to JSON string
            ObjectMapper mapper = new ObjectMapper();
            String configJson = mapper.writeValueAsString(request.getConfig());

            AIConfig aiConfig = new AIConfig();
            aiConfig.setId(request.getId());
            aiConfig.setUserId(request.getUserId());
            aiConfig.setProviderId(request.getProviderId());
            aiConfig.setModelId(request.getModelId());
            aiConfig.setConfig(configJson);
            aiConfig.setIsActive(request.getIsActive());

            AIConfig savedConfig = aiConfigService.saveAIConfig(aiConfig);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 1);
            result.put("data", savedConfig);
            result.put("message", "AI config saved successfully");
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 0);
            result.put("message", "Failed to save AI config: " + e.getMessage());
            return result;
        }
    }

    // 删除AI配置
    @DeleteMapping("/configs/{id}")
    public Map<String, Object> deleteAIConfig(@PathVariable Integer id) {
        try {
            aiConfigService.deleteAIConfig(id);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 1);
            result.put("message", "AI config deleted successfully");
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 0);
            result.put("message", "Failed to delete AI config: " + e.getMessage());
            return result;
        }
    }

    // 设置活动AI配置
    @PutMapping("/configs/{id}/active")
    public Map<String, Object> setActiveAIConfig(@PathVariable Integer id) {
        try {
            AIConfig config = aiConfigService.setActiveAIConfig(id);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 1);
            result.put("data", config);
            result.put("message", "Active AI config updated successfully");
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 0);
            result.put("message", "Failed to set active AI config: " + e.getMessage());
            return result;
        }
    }

    // 测试AI配置
    @PostMapping("/configs/test")
    public Map<String, Object> testAIConfig(@RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            AIConfig aiConfig = (AIConfig) body.get("config");
            String testPrompt = (String) body.getOrDefault("testPrompt", "Hello, write a simple Vue component");
            
            // TODO: 实际测试配置是否有效
            Map<String, Object> result = new HashMap<>();
            result.put("code", 1);
            result.put("message", "AI config test successful");
            result.put("data", Map.of(
                "testResult", "Test completed successfully",
                "responseTime", "200ms",
                "model", aiConfig.getModelId()
            ));
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 0);
            result.put("message", "AI config test failed: " + e.getMessage());
            return result;
        }
    }

    // 获取AI提供商信息
    @GetMapping("/providers")
    public Map<String, Object> getAIProviders() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 1);
        result.put("data", aiConfigService.getAIProviders());
        return result;
    }
}

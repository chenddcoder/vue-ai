package com.vueai.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AIGenerateService {

    private final Logger logger = LoggerFactory.getLogger(AIGenerateService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final Map<String, String> ZHIPU_MODELS = new HashMap<>();
    static {
        ZHIPU_MODELS.put("glm-4", "GLM-4");
        ZHIPU_MODELS.put("glm-4v", "GLM-4V");
        ZHIPU_MODELS.put("glm-3-turbo", "GLM-3-Turbo");
        ZHIPU_MODELS.put("chatglm_turbo", "ChatGLM-Turbo");
        ZHIPU_MODELS.put("chatglm_pro", "ChatGLM-Pro");
        ZHIPU_MODELS.put("chatglm_std", "ChatGLM-Std");
        ZHIPU_MODELS.put("chatglm_lite", "ChatGLM-Lite");
    }

    private String buildVueSystemPrompt() {
        return "You are a Vue 3 component generation expert. Return ONLY the complete Vue component code. " +
               "Use Vue 3 Composition API with <script setup> syntax. " +
               "Define reactive data with ref() or reactive(). " +
               "Define methods as regular functions. " +
               "Do not return JSON. Do not add markdown code blocks. Just return raw Vue component code.";
    }
    
    private String buildVueUserPrompt(String prompt) {
        return "Generate a Vue 3 component using <script setup>: " + prompt;
    }

    private List<Map<String, Object>> buildMessages(String prompt) {
        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", buildVueSystemPrompt());
        
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", buildVueUserPrompt(prompt));
        
        return Arrays.asList(systemMsg, userMsg);
    }

    public Map<String, Object> generate(String provider, String model, Map<String, Object> config, String prompt) throws Exception {
        logger.info("=== AI Generate Request ===");
        logger.info("Provider: {}, Model: {}", provider, model);
        logger.info("Prompt: {}", prompt);
        
        List<Map<String, Object>> messages = buildMessages(prompt);
        return generateWithMessages(provider, model, config, messages);
    }

    public Map<String, Object> generateWithMessages(String provider, String model, Map<String, Object> config, List<Map<String, Object>> messages) throws Exception {
        if (config.containsKey("modelName") && config.get("modelName") != null) {
            model = config.get("modelName").toString();
        }
        
        String providerId = provider.toLowerCase();
        
        if (providerId.equals("zhipu") || 
            providerId.contains("zhipu") || 
            providerId.equals("glm") ||
            (config.get("baseUrl") != null && config.get("baseUrl").toString().toLowerCase().contains("bigmodel.cn"))) {
            return generateWithZhipuAI(config, messages, model);
        }
        
        switch (providerId) {
            case "openai":
                return generateWithOpenAI(config, messages, model);
            case "anthropic":
                return generateWithAnthropic(config, messages, model);
            case "qwen":
                return generateWithQwen(config, messages, model);
            case "custom":
                return generateWithCustomAPI(config, messages, model);
            default:
                return generateWithCustomAPI(config, messages, model);
        }
    }

    private Map<String, Object> generateWithZhipuAI(Map<String, Object> config, List<Map<String, Object>> messages, String model) throws Exception {
        String apiKey = (String) config.get("apiKey");
        String baseUrl = (String) config.getOrDefault("baseUrl", "https://open.bigmodel.cn/api/paas/v4");
        
        String actualModel = model;
        if (!ZHIPU_MODELS.containsKey(model.toLowerCase()) && !model.toLowerCase().contains("glm")) {
            actualModel = "glm-4";
        }
        
        String url = baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "chat/completions";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", actualModel);
        body.put("messages", messages);
        body.put("max_tokens", 4096);
        body.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        logger.info("Calling ZhipuAI API: {}", url);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        logger.info("ZhipuAI Response: {}", response.getStatusCode());
        
        return parseZhipuAIResponse(response.getBody());
    }

    private Map<String, Object> generateWithOpenAI(Map<String, Object> config, List<Map<String, Object>> messages, String model) throws Exception {
        String apiKey = (String) config.get("apiKey");
        String baseUrl = (String) config.getOrDefault("baseUrl", "https://api.openai.com/v1");
        String url = baseUrl + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && apiKey.startsWith("sk-")) {
            headers.setBearerAuth(apiKey);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("max_tokens", 4096);
        body.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        logger.info("Calling OpenAI API: {}", url);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        logger.info("OpenAI Response: {}", response.getStatusCode());
        
        return parseOpenAIResponse(response.getBody());
    }

    private Map<String, Object> generateWithAnthropic(Map<String, Object> config, List<Map<String, Object>> messages, String model) throws Exception {
        String apiKey = (String) config.get("apiKey");
        String url = "https://api.anthropic.com/v1/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("max_tokens", 4096);
        body.put("messages", messages);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        logger.info("Calling Anthropic API: {}", url);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        logger.info("Anthropic Response: {}", response.getStatusCode());
        
        return parseAnthropicResponse(response.getBody());
    }

    private Map<String, Object> generateWithQwen(Map<String, Object> config, List<Map<String, Object>> messages, String model) throws Exception {
        String apiKey = (String) config.get("apiKey");
        String url = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("max_tokens", 4096);
        body.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        logger.info("Calling Qwen API: {}", url);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        logger.info("Qwen Response: {}", response.getStatusCode());
        
        return parseOpenAIResponse(response.getBody());
    }

    private Map<String, Object> generateWithCustomAPI(Map<String, Object> config, List<Map<String, Object>> messages, String model) throws Exception {
        String baseUrl = (String) config.get("baseUrl");
        String apiKey = (String) config.get("apiKey");

        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new RuntimeException("API地址未配置，请先配置API地址");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.setBearerAuth(apiKey);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("max_tokens", 4096);
        body.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        logger.info("Calling Custom API: {}", baseUrl);
        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, request, String.class);
        logger.info("Custom API Response: {}", response.getStatusCode());
        
        return parseOpenAIResponse(response.getBody());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseZhipuAIResponse(String responseBody) throws Exception {
        Map response = objectMapper.readValue(responseBody, Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("API返回空响应");
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");
        if (content == null) content = (String) message.get("reasoning_content");
        
        // 清理可能的markdown代码块
        content = cleanMarkdownCodeBlock(content);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 1);
        Map<String, Object> data = new HashMap<>();
        data.put("content", content);
        result.put("data", data);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseOpenAIResponse(String responseBody) throws Exception {
        Map response = objectMapper.readValue(responseBody, Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("API返回空响应");
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");
        if (content == null) content = (String) message.get("reasoning_content");
        
        // 清理可能的markdown代码块
        content = cleanMarkdownCodeBlock(content);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 1);
        Map<String, Object> data = new HashMap<>();
        data.put("content", content);
        result.put("data", data);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAnthropicResponse(String responseBody) throws Exception {
        Map response = objectMapper.readValue(responseBody, Map.class);
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        if (content == null || content.isEmpty()) {
            throw new RuntimeException("API返回空响应");
        }
        String contentText = (String) content.get(0).get("text");
        
        // 清理可能的markdown代码块
        contentText = cleanMarkdownCodeBlock(contentText);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 1);
        Map<String, Object> data = new HashMap<>();
        data.put("content", contentText);
        result.put("data", data);
        return result;
    }
    
    private String cleanMarkdownCodeBlock(String content) {
        if (content == null) return "";
        // 移除 ```vue, ```html, ```xml 等代码块标记
        content = content.replaceAll("```vue\\s*", "");
        content = content.replaceAll("```html\\s*", "");
        content = content.replaceAll("```xml\\s*", "");
        content = content.replaceAll("```typescript\\s*", "");
        content = content.replaceAll("```javascript\\s*", "");
        content = content.replaceAll("```\\s*", "");
        // 移除结尾的 ```
        content = content.replaceAll("\\s*```\\s*$", "");
        return content.trim();
    }

    private Map<String, Object> parseAIContent(String content) throws Exception {
        return null;
    }
}

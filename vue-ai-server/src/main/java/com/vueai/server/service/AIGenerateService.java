package com.vueai.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AIGenerateService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 智谱AI支持的模型列表
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

public Map<String, Object> generate(String provider, String model, Map<String, Object> config, String prompt) {
        // 打印调试信息
        System.out.println("=== AI Generate Request ===");
        System.out.println("Provider: " + provider);
        System.out.println("Model (from field): " + model);
        System.out.println("Config: " + config);
        
        // 如果config中有modelName，优先使用
        if (config.containsKey("modelName") && config.get("modelName") != null) {
            model = config.get("modelName").toString();
            System.out.println("Model (from config.modelName): " + model);
        }
        
        // 转换为小写进行provider匹配
        String providerId = provider.toLowerCase();
        
        if (providerId.equals("zhipu") || 
            providerId.contains("zhipu") || 
            providerId.equals("glm") ||
            (config.get("baseUrl") != null && config.get("baseUrl").toString().toLowerCase().contains("bigmodel.cn"))) {
            return generateWithZhipuAI(config, prompt, model);
        }
        
        switch (providerId) {
            case "openai":
                return generateWithOpenAI(config, prompt, model);
            case "anthropic":
                return generateWithAnthropic(config, prompt, model);
            case "custom":
                return generateWithCustomAPI(config, prompt, model);
            default:
                return generateWithCustomAPI(config, prompt, model);
        }
    }

    private Map<String, Object> generateWithZhipuAI(Map<String, Object> config, String prompt, String model) {
        String apiKey = (String) config.get("apiKey");
        String baseUrl = (String) config.getOrDefault("baseUrl", "https://open.bigmodel.cn/api/paas/v4");
        
        System.out.println("=== ZhipuAI Request ===");
        System.out.println("API Key: " + (apiKey != null ? "***" : "null"));
        System.out.println("Base URL: " + baseUrl);
        System.out.println("Model to use: " + model);
        
        // 如果模型名称不是智谱AI的标准模型，尝试使用 glm-4
        String actualModel = model;
        if (!ZHIPU_MODELS.containsKey(model.toLowerCase()) && !model.toLowerCase().contains("glm")) {
            actualModel = "glm-4";
            System.out.println("Using default ZhipuAI model: glm-4");
        }
        
        String url = baseUrl;
        if (!url.endsWith("/")) {
            url += "/";
        }
        url += "chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", apiKey);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> body = new HashMap<>();
        body.put("model", actualModel);
        body.put("messages", Collections.singletonList(message));
        body.put("max_tokens", 4096);
        body.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            System.out.println("Calling ZhipuAI API: " + url + " with model: " + actualModel);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            System.out.println("ZhipuAI Response: " + response.getBody());
            return parseZhipuAIResponse(response.getBody());
        } catch (Exception e) {
            System.err.println("ZhipuAI API Error: " + e.getMessage());
            e.printStackTrace();
            return generateMockResponse(prompt, "ZhipuAI", actualModel);
        }
    }

    private Map<String, Object> generateWithOpenAI(Map<String, Object> config, String prompt, String model) {
        String apiKey = (String) config.get("apiKey");
        String baseUrl = (String) config.getOrDefault("baseUrl", "https://api.openai.com/v1");

        String url = baseUrl + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && apiKey.startsWith("sk-")) {
            headers.setBearerAuth(apiKey);
        }

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", Collections.singletonList(message));
        body.put("max_tokens", 4096);
        body.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            return parseOpenAIResponse(response.getBody());
        } catch (Exception e) {
            System.err.println("OpenAI API Error: " + e.getMessage());
            return generateMockResponse(prompt, "OpenAI", model);
        }
    }

    private Map<String, Object> generateWithAnthropic(Map<String, Object> config, String prompt, String model) {
        String apiKey = (String) config.get("apiKey");
        String url = "https://api.anthropic.com/v1/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("max_tokens", 4096);
        body.put("messages", Collections.singletonList(Map.of("role", "user", "content", prompt)));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            return parseAnthropicResponse(response.getBody());
        } catch (Exception e) {
            System.err.println("Anthropic API Error: " + e.getMessage());
            return generateMockResponse(prompt, "Anthropic", model);
        }
    }

    private Map<String, Object> generateWithCustomAPI(Map<String, Object> config, String prompt, String model) {
        String baseUrl = (String) config.get("baseUrl");
        String apiKey = (String) config.get("apiKey");

        if (baseUrl == null || baseUrl.isEmpty()) {
            return generateMockResponse(prompt, "Custom", model);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (apiKey != null && !apiKey.isEmpty()) {
            headers.setBearerAuth(apiKey);
        }

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", Collections.singletonList(message));
        body.put("max_tokens", 4096);
        body.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            System.out.println("Calling Custom API: " + baseUrl);
            ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, request, String.class);
            System.out.println("Custom API Response: " + response.getBody());
            return parseOpenAIResponse(response.getBody());
        } catch (Exception e) {
            System.err.println("Custom API Error: " + e.getMessage());
            e.printStackTrace();
            return generateMockResponse(prompt, "Custom", model);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseZhipuAIResponse(String responseBody) {
        try {
            Map response = objectMapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                return generateMockResponse("", "ZhipuAI", "glm-4");
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");
            return parseAIContent(content);
        } catch (Exception e) {
            System.err.println("ZhipuAI Parse Error: " + e.getMessage());
            return generateMockResponse("", "ZhipuAI", "glm-4");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseOpenAIResponse(String responseBody) {
        try {
            Map response = objectMapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                return generateMockResponse("", "OpenAI", "gpt-4");
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");
            
            // 如果content为空，尝试从reasoning_content获取
            if (content == null || content.isEmpty()) {
                content = (String) message.get("reasoning_content");
            }
            
            return parseAIContent(content);
        } catch (Exception e) {
            System.err.println("OpenAI Parse Error: " + e.getMessage());
            e.printStackTrace();
            return generateMockResponse("", "OpenAI", "gpt-4");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAnthropicResponse(String responseBody) {
        try {
            Map response = objectMapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
            if (content == null || content.isEmpty()) {
                return generateMockResponse("", "Anthropic", "claude-3-opus");
            }

            String contentText = (String) content.get(0).get("text");
            return parseAIContent(contentText);
        } catch (Exception e) {
            System.err.println("Anthropic Parse Error: " + e.getMessage());
            return generateMockResponse("", "Anthropic", "claude-3-opus");
        }
    }

    private Map<String, Object> parseAIContent(String content) {
        try {
            return objectMapper.readValue(content, Map.class);
        } catch (Exception e) {
            return extractFromCodeBlocks(content);
        }
    }

    private Map<String, Object> extractFromCodeBlocks(String content) {
        Map<String, Object> result = new HashMap<>();

        Pattern templatePattern = Pattern.compile("<template>(.*?)</template>", Pattern.DOTALL);
        Pattern scriptPattern = Pattern.compile("<script>(.*?)</script>", Pattern.DOTALL);
        Pattern stylePattern = Pattern.compile("<style.*?>(.*?)</style>", Pattern.DOTALL);

        Matcher templateMatcher = templatePattern.matcher(content);
        Matcher scriptMatcher = scriptPattern.matcher(content);
        Matcher styleMatcher = stylePattern.matcher(content);

        String template = "<template><div>Generated Component</div></template>";
        String script = "export default { data() { return {} } }";
        String style = "";

        if (templateMatcher.find()) {
            template = "<template>" + templateMatcher.group(1).trim() + "</template>";
        }
        if (scriptMatcher.find()) {
            script = scriptMatcher.group(1).trim();
        }
        if (styleMatcher.find()) {
            style = styleMatcher.group(1).trim();
        }

        result.put("template", template);
        result.put("methods", script);
        result.put("style", style);
        result.put("usage", Map.of("promptTokens", 100, "completionTokens", 500, "totalTokens", 600));

        return result;
    }

    private Map<String, Object> generateMockResponse(String prompt, String provider, String model) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> data = new HashMap<>();

        String escapedPrompt = prompt.length() > 30 ? prompt.substring(0, 30) + "..." : prompt;

        String template = "<template>\n" +
                "  <div class=\"generated-component\">\n" +
                "    <a-card title=\"" + escapedPrompt + "\">\n" +
                "      <p>This is an AI generated component.</p>\n" +
                "      <p>Provider: " + provider + "</p>\n" +
                "      <p>Model: " + model + "</p>\n" +
                "      <a-button type=\"primary\" @click=\"handleClick\">Click Me</a-button>\n" +
                "    </a-card>\n" +
                "  </div>\n" +
                "</template>";

        String script = "export default {\n" +
                "  data() {\n" +
                "    return {\n" +
                "      message: 'Hello from AI!'\n" +
                "    }\n" +
                "  },\n" +
                "  methods: {\n" +
                "    handleClick() {\n" +
                "      this.$message.success('Button clicked!')\n" +
                "    }\n" +
                "  }\n" +
                "}";

        String style = ".generated-component {\n" +
                "  padding: 20px;\n" +
                "}";

        data.put("template", template);
        data.put("methods", script);
        data.put("style", style);

        Map<String, Integer> usage = new HashMap<>();
        usage.put("promptTokens", 50);
        usage.put("completionTokens", 300);
        usage.put("totalTokens", 350);
        data.put("usage", usage);

        result.put("data", data);
        return result;
    }
}

package com.vueai.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vueai.server.model.AIConfig;
import com.vueai.server.util.ProjectContextAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.transaction.annotation.Transactional;

@Service
public class SmartCodeGeneratorService {

    private final Logger logger = LoggerFactory.getLogger(SmartCodeGeneratorService.class);
    
    @Autowired
    private AIGenerateService aiGenerateService;
    
    @Autowired
    private AIConfigService aiConfigService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generateModule(Integer projectId, String requirement, Integer userId, Map<String, String> currentFiles) throws Exception {
        Object contentObj;
        
        // 1. 获取项目信息 (优先使用前端传来的文件)
        if (currentFiles != null && !currentFiles.isEmpty()) {
            contentObj = currentFiles;
        } else if (projectId != null && projectId > 0) {
            Map<String, Object> project = jdbcTemplate.queryForMap("SELECT * FROM magic_sys_project WHERE id = ?", projectId);
            contentObj = (String) project.get("content");
        } else {
            throw new RuntimeException("未提供项目文件内容或有效的项目ID");
        }
        
        // 2. 获取AI配置
        AIConfig aiConfig = aiConfigService.getActiveAIConfig(userId);
        if (aiConfig == null) {
            throw new RuntimeException("未找到活动的AI配置");
        }
        
        // 3. 分析项目上下文
        String structure = ProjectContextAnalyzer.analyzeStructure(contentObj);
        String dependencies = ProjectContextAnalyzer.getDependencies(contentObj);
        String gitIgnore = ProjectContextAnalyzer.findFileContent(contentObj, ".gitignore");
        
        // 4. 构建提示词
        List<Map<String, Object>> messages = buildPrompt(structure, dependencies, requirement);
        
        // 5. 调用AI
        Map<String, Object> configMap = objectMapper.readValue(aiConfig.getConfig(), Map.class);
        Map<String, Object> aiResponse = aiGenerateService.generateWithMessages(
            aiConfig.getProviderId(), 
            aiConfig.getModelId(), 
            configMap, 
            messages
        );
        
        String generatedContent = (String) ((Map) aiResponse.get("data")).get("content");
        
        // 6. 解析生成的文件
        List<Map<String, String>> files = parseGeneratedContent(generatedContent);
        
        // 7. 代码质量与规范检查
        validateGeneratedFiles(files, dependencies, gitIgnore);
        
        // 8. 更新项目文件 (仅当有项目ID时才保存到数据库)
        if (projectId != null && projectId > 0) {
            try {
                // 如果使用的是flat map (currentFiles)，我们需要先将其转换为tree结构，或者如果contentObj是json string，则按原逻辑
                List<Map<String, Object>> projectFiles;
                if (contentObj instanceof Map) {
                    // 如果源是Map，说明我们没有原始的Tree结构。
                    // 这种情况下，我们不需要在这里重建Tree并保存，因为前端会接收文件并更新它的Store。
                    // 只有当前端显式保存项目时，才会将Store中的Files转换为Tree保存到DB。
                    // 所以这里我们其实可以跳过DB更新，或者我们需要从DB重新读取一次Tree来更新。
                    
                    // 为了保持一致性，如果projectId存在，我们应该尝试更新DB。
                    // 重新从DB读取最新的Tree结构
                    Map<String, Object> project = jdbcTemplate.queryForMap("SELECT * FROM magic_sys_project WHERE id = ?", projectId);
                    String dbContentJson = (String) project.get("content");
                    projectFiles = objectMapper.readValue(dbContentJson, new TypeReference<List<Map<String, Object>>>(){});
                } else {
                    projectFiles = objectMapper.readValue((String) contentObj, new TypeReference<List<Map<String, Object>>>(){});
                }
                
                updateProjectFiles(projectFiles, files);
                
                // 9. 保存回数据库
                String newContentJson = objectMapper.writeValueAsString(projectFiles);
                jdbcTemplate.update("UPDATE magic_sys_project SET content = ? WHERE id = ?", newContentJson, projectId);
            } catch (Exception e) {
                logger.warn("Failed to auto-save project files to DB: " + e.getMessage());
                // Don't fail the request if DB save fails, as the user still gets the generated code
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("files", files);
        result.put("message", "生成成功" + (projectId != null ? "，已更新项目文件" : ""));
        return result;
    }
    
    private List<Map<String, Object>> buildPrompt(String structure, String dependencies, String requirement) {
        String systemPrompt = "你是一个高级全栈工程师和Vue 3专家。你的任务是根据项目上下文和需求生成代码。\n" +
            "必须遵守以下规则：\n" +
            "1. 生成的代码必须符合Vue 3 Composition API规范，使用<script setup>。\n" +
            "2. 使用TypeScript。\n" +
            "3. 严格遵循项目现有的目录结构和命名规范。\n" +
            "4. 注意：项目根目录即为源代码目录。App.vue 是入口页面（位于根目录）。生成文件路径时不要包含 'src/' 前缀。例如：'views/Home.vue', 'components/Header.vue'。\n" +
            "5. 返回格式必须是XML格式，包含多个文件，如下所示：\n" +
            "<response>\n" +
            "  <file path=\"views/NewPage.vue\">\n" +
            "    <![CDATA[\n" +
            "      <template>...</template>\n" +
            "      <script setup>...</script>\n" +
            "    ]]>\n" +
            "  </file>\n" +
            "  <file path=\"api/new-api.ts\">\n" +
            "    <![CDATA[\n" +
            "      export const ...\n" +
            "    ]]>\n" +
            "  </file>\n" +
            "</response>\n" +
            "6. 不要返回任何Markdown代码块标记（如```xml），只返回XML内容。\n" +
            "7. 确保生成的代码完整、可运行，包含所有必要的导入。\n" +
            "8. 如果是简单工具或单页面应用，**不要**使用 vue-router，请直接在 App.vue 中引入并渲染主要组件（如 <JsonFormatter />）。只有在确实需要多页面导航时才使用路由，并且必须生成 router/index.ts 配置文件。";
            
        String userPrompt = "项目目录结构：\n" + structure + "\n\n" +
            "项目依赖(package.json)：\n" + dependencies + "\n\n" +
            "开发需求：\n" + requirement;
            
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> sysMsg = new HashMap<>();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt);
        messages.add(sysMsg);
        
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.add(userMsg);
        
        return messages;
    }
    
    private List<Map<String, String>> parseGeneratedContent(String content) {
        List<Map<String, String>> files = new ArrayList<>();
        
        // 简单的XML解析，使用正则
        Pattern filePattern = Pattern.compile("<file path=\"([^\"]+)\">\\s*(?:<!\\[CDATA\\[)?(.*?)(?:\\]\\]>)?\\s*</file>", Pattern.DOTALL);
        Matcher matcher = filePattern.matcher(content);
        
        while (matcher.find()) {
            Map<String, String> file = new HashMap<>();
            String path = matcher.group(1);
            
            // 如果AI不听话，仍然生成了src/前缀，我们强制去掉它
            if (path.startsWith("src/")) {
                path = path.substring(4);
            }
            
            file.put("path", path);
            file.put("content", matcher.group(2).trim());
            files.add(file);
        }
        
        if (files.isEmpty()) {
            // 尝试备用解析：如果AI没有返回XML，可能是返回了单个代码块
            // 这里可以添加更智能的降级处理，目前先简单返回
            logger.warn("未解析到XML格式文件，原始内容：{}", content);
        }
        
        return files;
    }
    
    private void updateProjectFiles(List<Map<String, Object>> projectTree, List<Map<String, String>> newFiles) {
        for (Map<String, String> newFile : newFiles) {
            String path = newFile.get("path");
            String content = newFile.get("content");
            String[] parts = path.split("/");
            
            List<Map<String, Object>> currentLevel = projectTree;
            
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                boolean isFile = (i == parts.length - 1);
                
                Map<String, Object> existingNode = findNode(currentLevel, part);
                
                if (existingNode != null) {
                    if (isFile) {
                        // 更新文件内容
                        existingNode.put("content", content);
                    } else {
                        // 进入下一级目录
                        if (!existingNode.containsKey("children")) {
                            existingNode.put("children", new ArrayList<>());
                        }
                        currentLevel = (List<Map<String, Object>>) existingNode.get("children");
                    }
                } else {
                    // 创建新节点
                    Map<String, Object> newNode = new HashMap<>();
                    newNode.put("name", part);
                    newNode.put("type", isFile ? "file" : "folder"); // 这里的类型要跟前端一致
                    if (isFile) {
                        newNode.put("content", content);
                    } else {
                        newNode.put("children", new ArrayList<>());
                    }
                    currentLevel.add(newNode);
                    
                    if (!isFile) {
                        currentLevel = (List<Map<String, Object>>) newNode.get("children");
                    }
                }
            }
        }
    }
    
    private Map<String, Object> findNode(List<Map<String, Object>> nodes, String name) {
        if (nodes == null) return null;
        for (Map<String, Object> node : nodes) {
            if (name.equals(node.get("name"))) {
                return node;
            }
        }
        return null;
    }

    private void validateGeneratedFiles(List<Map<String, String>> files, String packageJsonContent, String gitIgnoreContent) {
        // 1. Naming Convention
        for (Map<String, String> file : files) {
            String path = file.get("path");
            String fileName = path.substring(path.lastIndexOf("/") + 1);
            if (path.endsWith(".vue") && !Character.isUpperCase(fileName.charAt(0))) {
                 throw new RuntimeException("违反命名规范: Vue组件文件名必须大写开头 (" + fileName + ")");
            }
        }

        // 2. GitIgnore Check
        if (gitIgnoreContent != null && !gitIgnoreContent.isEmpty()) {
             List<String> ignoredPatterns = Arrays.asList(gitIgnoreContent.split("\n"));
             for (Map<String, String> file : files) {
                 String path = file.get("path");
                 for (String pattern : ignoredPatterns) {
                     pattern = pattern.trim();
                     if (pattern.isEmpty() || pattern.startsWith("#")) continue;
                     if (simpleGlobMatch(path, pattern)) {
                          throw new RuntimeException("违反版本控制规则: 文件被.gitignore忽略 (" + path + " 匹配规则 " + pattern + ")");
                     }
                 }
             }
        }
         
        // 3. Dependency Check - 开放平台：只检测不自动添加，提示用户提交心愿单
        Set<String> missingDeps = new HashSet<>();
        if (packageJsonContent != null) {
            try {
                Map<String, Object> pkg = objectMapper.readValue(packageJsonContent, Map.class);
                Map<String, Object> dependencies = pkg.containsKey("dependencies") ? (Map<String, Object>) pkg.get("dependencies") : new HashMap<>();
                
                Pattern importPattern = Pattern.compile("import\\s+.*?\\s+from\\s+['\"]([^'\"]+)['\"]");
                for (Map<String, String> file : files) {
                    Matcher matcher = importPattern.matcher(file.get("content"));
                    while (matcher.find()) {
                        String importPath = matcher.group(1);
                        if (importPath.startsWith(".") || importPath.startsWith("@") || importPath.startsWith("/")) continue;
                        
                        String pkgName = importPath;
                        if (pkgName.startsWith("@")) {
                             String[] parts = pkgName.split("/");
                             if (parts.length >= 2) pkgName = parts[0] + "/" + parts[1];
                        } else {
                             if (pkgName.contains("/")) pkgName = pkgName.split("/")[0];
                        }
                        
                        // 检测未预置的依赖
                        if (!dependencies.containsKey(pkgName) && !isBuiltIn(pkgName)) {
                            missingDeps.add(pkgName);
                        }
                    }
                }
                
                if (!missingDeps.isEmpty()) {
                    String depList = String.join(", ", missingDeps);
                    throw new RuntimeException("依赖未预置: " + depList + "。请通过「应用市场」→「心愿单」提交新依赖申请，管理员审核后将在24小时内添加。");
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                logger.warn("依赖验证跳过: " + e.getMessage());
            }
        }
    }
    
    private boolean simpleGlobMatch(String path, String pattern) {
        pattern = pattern.trim();
        if (pattern.startsWith("/")) pattern = pattern.substring(1);
        if (pattern.endsWith("/")) {
            return path.startsWith(pattern) || path.contains("/" + pattern);
        }
        if (pattern.contains("*")) {
             String regex = pattern.replace(".", "\\.").replace("*", ".*");
             return path.matches(".*" + regex + ".*");
        }
        return path.equals(pattern) || path.endsWith("/" + pattern) || path.contains("/" + pattern + "/");
    }

    // 预置常用依赖列表（开放平台支持这些依赖）
    private static final Set<String> BUILT_IN_DEPS = new HashSet<>(Arrays.asList(
        "vue", "vue-router", "pinia", "ant-design-vue", "axios",
        "file-saver", "lodash", "lodash-es", "dayjs", "clsx",
        "echarts", "nprogress", "qrcode", "jszip", "dompurify",
        "sass", "less", "tailwindcss", "mockjs"
    ));
    
    private boolean isBuiltIn(String pkgName) {
        return BUILT_IN_DEPS.contains(pkgName);
    }
}

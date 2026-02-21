package com.vueai.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/market")
@CrossOrigin
public class MarketController {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 获取应用列表（增强搜索）
     */
    @GetMapping("/apps")
    public Map<String, Object> getApps(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        try {
            StringBuilder sql = new StringBuilder(
                "SELECT id, project_id, name, description, tags, thumbnail, " +
                "author_id, author_name, author_avatar, likes, views, publish_time, is_open_source, version " +
                "FROM magic_sys_market_app WHERE status = 1"
            );
            List<Object> params = new ArrayList<>();
            
            // 关键词搜索 - 增强版：支持名称、描述、标签、作者名搜索
            if (keyword != null && !keyword.isEmpty()) {
                sql.append(" AND (name LIKE ? OR description LIKE ? OR tags LIKE ? OR author_name LIKE ?)");
                String kw = "%" + keyword + "%";
                params.add(kw);
                params.add(kw);
                params.add(kw);
                params.add(kw);
            }
            
            // 分类筛选
            if (category != null && !category.isEmpty()) {
                sql.append(" AND tags LIKE ?");
                params.add("%" + category + "%");
            }
            
            // 排序方式
            if ("popular".equals(sort)) {
                sql.append(" ORDER BY likes DESC, views DESC");
            } else if ("latest".equals(sort)) {
                sql.append(" ORDER BY publish_time DESC");
            } else if ("trending".equals(sort)) {
                sql.append(" ORDER BY views DESC");
            } else {
                sql.append(" ORDER BY publish_time DESC");
            }
            
            // 获取总数
            String countSql = sql.toString().replace(
                "SELECT id, project_id, name, description, tags, thumbnail, author_id, author_name, author_avatar, likes, views, publish_time, is_open_source, version",
                "SELECT COUNT(*)"
            );
            Integer total = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());
            
            // 分页查询
            sql.append(" LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add((page - 1) * pageSize);
            
            List<Map<String, Object>> apps = jdbcTemplate.queryForList(sql.toString(), params.toArray());
            
            // 解析 tags
            for (Map<String, Object> app : apps) {
                String tagsStr = (String) app.get("tags");
                if (tagsStr != null && !tagsStr.isEmpty()) {
                    app.put("tags", tagsStr.split(","));
                } else {
                    app.put("tags", new String[0]);
                }
            }
            
            result.put("code", 200);
            Map<String, Object> data = new HashMap<>();
            data.put("list", apps);
            data.put("total", total != null ? total : 0);
            data.put("page", page);
            data.put("pageSize", pageSize);
            result.put("data", data);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取热门搜索词
     */
    @GetMapping("/search/hot")
    public Map<String, Object> getHotSearch() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 获取热门标签
            List<Map<String, Object>> hotTags = jdbcTemplate.queryForList(
                "SELECT tag, COUNT(*) as count FROM (" +
                "SELECT tags FROM magic_sys_market_app WHERE status = 1" +
                ") CROSS JOIN JSON_TABLE(REPLACE(tags, ',', ','), '$[*]' COLUMNS (tag PATH '$')) " +
                "GROUP BY tag ORDER BY count DESC LIMIT 10"
            );
            
            // 获取热门应用名称作为搜索建议
            List<Map<String, Object>> popularApps = jdbcTemplate.queryForList(
                "SELECT DISTINCT name FROM magic_sys_market_app WHERE status = 1 ORDER BY likes DESC LIMIT 5"
            );
            
            result.put("code", 200);
            Map<String, Object> data = new HashMap<>();
            data.put("hotTags", hotTags);
            data.put("suggestions", popularApps);
            result.put("data", data);
        } catch (Exception e) {
            // 如果SQL解析失败，返回静态数据
            result.put("code", 200);
            Map<String, Object> data = new HashMap<>();
            data.put("hotTags", new String[]{"工具", "游戏", "展示", "学习", "效率"});
            data.put("suggestions", new String[]{"计算器", "待办事项", "贪吃蛇", "天气预报", "音乐播放器"});
            result.put("data", data);
        }
        return result;
    }

    /**
     * 智能推荐 - 基于用户行为
     */
    @GetMapping("/recommend")
    public Map<String, Object> getRecommendedApps(
            @RequestParam(required = false) Integer userId,
            @RequestParam(defaultValue = "6") Integer limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> recommendations = new ArrayList<>();
            
            if (userId != null) {
                // 基于用户收藏的标签推荐
                String favoriteTagsSql = 
                    "SELECT m.tags FROM magic_sys_market_app_favorite f " +
                    "JOIN magic_sys_market_app m ON f.app_id = m.id " +
                    "WHERE f.user_id = ?";
                
                List<Map<String, Object>> favoriteApps = jdbcTemplate.queryForList(favoriteTagsSql, userId);
                
                if (!favoriteApps.isEmpty()) {
                    // 收集用户喜欢的标签
                    Set<String> preferredTags = new HashSet<>();
                    for (Map<String, Object> app : favoriteApps) {
                        String tags = (String) app.get("tags");
                        if (tags != null && !tags.isEmpty()) {
                            for (String tag : tags.split(",")) {
                                preferredTags.add(tag.trim().toLowerCase());
                            }
                        }
                    }
                    
                    // 基于偏好标签推荐
                    if (!preferredTags.isEmpty()) {
                        StringBuilder recSql = new StringBuilder(
                            "SELECT id, project_id, name, description, tags, thumbnail, " +
                            "author_id, author_name, author_avatar, likes, views, publish_time, version " +
                            "FROM magic_sys_market_app WHERE status = 1 AND id NOT IN (" +
                            "SELECT app_id FROM magic_sys_market_app_favorite WHERE user_id = ?) AND ("
                        );
                        List<Object> params = new ArrayList<>();
                        params.add(userId);
                        
                        int tagCount = 0;
                        for (String tag : preferredTags) {
                            if (tagCount > 0) recSql.append(" OR ");
                            recSql.append("LOWER(tags) LIKE ?");
                            params.add("%" + tag + "%");
                            tagCount++;
                        }
                        recSql.append(") ORDER BY likes DESC LIMIT ?");
                        params.add(limit);
                        
                        recommendations = jdbcTemplate.queryForList(recSql.toString(), params.toArray());
                    }
                }
            }
            
            // 如果推荐结果不足，补充热门应用
            if (recommendations.size() < limit) {
                int needCount = limit - recommendations.size();
                Set<Integer> existingIds = new HashSet<>();
                for (Map<String, Object> app : recommendations) {
                    existingIds.add((Integer) app.get("id"));
                }
                
                String fallbackSql = 
                    "SELECT id, project_id, name, description, tags, thumbnail, " +
                    "author_id, author_name, author_avatar, likes, views, publish_time, version " +
                    "FROM magic_sys_market_app WHERE status = 1";
                if (!existingIds.isEmpty()) {
                    fallbackSql += " AND id NOT IN (" + 
                        existingIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")) + ")";
                }
                fallbackSql += " ORDER BY likes DESC LIMIT ?";
                
                List<Map<String, Object>> popularApps = jdbcTemplate.queryForList(fallbackSql, needCount);
                recommendations.addAll(popularApps);
            }
            
            // 解析 tags
            for (Map<String, Object> app : recommendations) {
                String tagsStr = (String) app.get("tags");
                if (tagsStr != null && !tagsStr.isEmpty()) {
                    app.put("tags", tagsStr.split(","));
                } else {
                    app.put("tags", new String[0]);
                }
            }
            
            result.put("code", 200);
            result.put("data", recommendations);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取应用详情
     */
    @GetMapping("/apps/{id}")
    public Map<String, Object> getAppDetail(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 增加浏览量
            jdbcTemplate.update("UPDATE magic_sys_market_app SET views = views + 1 WHERE id = ?", id);
            
            List<Map<String, Object>> apps = jdbcTemplate.queryForList(
                "SELECT * FROM magic_sys_market_app WHERE id = ? AND status = 1",
                id
            );
            
            if (apps.isEmpty()) {
                result.put("code", 404);
                result.put("message", "应用不存在");
            } else {
                Map<String, Object> app = apps.get(0);
                // 解析 tags
                String tagsStr = (String) app.get("tags");
                if (tagsStr != null && !tagsStr.isEmpty()) {
                    app.put("tags", tagsStr.split(","));
                } else {
                    app.put("tags", new String[0]);
                }
                // 解析 content
                String contentStr = (String) app.get("content");
                if (contentStr != null && !contentStr.isEmpty()) {
                    app.put("content", objectMapper.readValue(contentStr, Map.class));
                }
                
                result.put("code", 200);
                result.put("data", app);
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 发布或更新应用到市场
     */
    @PostMapping("/publish")
    public Map<String, Object> publishApp(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer projectId = body.get("projectId") != null ? 
                Integer.parseInt(body.get("projectId").toString()) : null;
            Integer appId = body.get("appId") != null ? 
                Integer.parseInt(body.get("appId").toString()) : null;
            String name = (String) body.get("name");
            String description = (String) body.get("description");
            String content = objectMapper.writeValueAsString(body.get("content"));
            String updateContent = (String) body.get("updateContent");
            String versionType = body.get("versionType") != null ? (String) body.get("versionType") : "patch";
            
            // 获取作者信息
            Integer authorId = body.get("authorId") != null ? 
                Integer.parseInt(body.get("authorId").toString()) : 1;
            String authorName = (String) body.get("authorName");
            if (authorName == null || authorName.isEmpty()) {
                authorName = "匿名用户";
            }
            
            // 处理 tags
            @SuppressWarnings("unchecked")
            List<String> tagsList = (List<String>) body.get("tags");
            String tags = tagsList != null ? String.join(",", tagsList) : "";
            
            // 是否开源
            Integer isOpenSource = body.get("isOpenSource") != null ? 
                (Boolean.TRUE.equals(body.get("isOpenSource")) ? 1 : 0) : 1;
            
            if (appId != null) {
                // 更新现有应用
                Map<String, Object> existingApp = jdbcTemplate.queryForMap(
                    "SELECT * FROM magic_sys_market_app WHERE id = ?", appId);
                
                // 保存当前版本到历史记录
                jdbcTemplate.update(
                    "INSERT INTO magic_sys_market_app_version (app_id, version, version_code, content, description, author_id, author_name) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    appId, 
                    existingApp.get("version"),
                    existingApp.get("version_code"),
                    existingApp.get("content"),
                    updateContent,
                    existingApp.get("author_id"),
                    existingApp.get("author_name")
                );
                
                // 计算新版本号
                String currentVersion = (String) existingApp.get("version");
                int currentCode = existingApp.get("version_code") != null ? 
                    ((Number) existingApp.get("version_code")).intValue() : 1;
                String newVersion = incrementVersion(currentVersion, versionType);
                
                // 更新应用
                jdbcTemplate.update(
                    "UPDATE magic_sys_market_app SET name = ?, description = ?, tags = ?, content = ?, " +
                    "is_open_source = ?, version = ?, version_code = ?, update_content = ?, publish_time = CURRENT_TIMESTAMP " +
                    "WHERE id = ?",
                    name, description, tags, content, isOpenSource, newVersion, currentCode + 1, 
                    updateContent, appId
                );
                
                result.put("code", 200);
                Map<String, Object> data = new HashMap<>();
                data.put("success", true);
                data.put("appId", appId);
                data.put("version", newVersion);
                data.put("message", "应用更新成功！新版本: " + newVersion);
                result.put("data", data);
            } else {
                // 新发布
                jdbcTemplate.update(
                    "INSERT INTO magic_sys_market_app (project_id, name, description, tags, content, author_id, author_name, is_open_source, version, version_code) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, '1.0.0', 1)",
                    projectId, name, description, tags, content, authorId, authorName, isOpenSource
                );
                
                Integer newAppId = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Integer.class);
                
                // 保存初始版本
                jdbcTemplate.update(
                    "INSERT INTO magic_sys_market_app_version (app_id, version, version_code, content, description, author_id, author_name) " +
                    "VALUES (?, '1.0.0', 1, ?, ?, ?, ?)",
                    newAppId, content, "首次发布", authorId, authorName
                );
                
                result.put("code", 200);
                Map<String, Object> data = new HashMap<>();
                data.put("success", true);
                data.put("appId", newAppId);
                data.put("version", "1.0.0");
                data.put("message", "发布成功");
                result.put("data", data);
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取应用版本历史
     */
    @GetMapping("/apps/{id}/versions")
    public Map<String, Object> getAppVersions(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> versions = jdbcTemplate.queryForList(
                "SELECT * FROM magic_sys_market_app_version WHERE app_id = ? ORDER BY version_code DESC",
                id
            );
            
            result.put("code", 200);
            result.put("data", versions);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 回滚到指定版本
     */
    @PostMapping("/apps/{appId}/rollback/{versionId}")
    public Map<String, Object> rollbackApp(
            @PathVariable Integer appId, 
            @PathVariable Integer versionId,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 获取要回滚的版本
            Map<String, Object> targetVersion = jdbcTemplate.queryForMap(
                "SELECT * FROM magic_sys_market_app_version WHERE app_id = ? AND id = ?",
                appId, versionId
            );
            
            // 获取当前应用信息
            Map<String, Object> currentApp = jdbcTemplate.queryForMap(
                "SELECT * FROM magic_sys_market_app WHERE id = ?", appId);
            
            // 保存当前版本到历史
            jdbcTemplate.update(
                "INSERT INTO magic_sys_market_app_version (app_id, version, version_code, content, description, author_id, author_name) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                appId, 
                currentApp.get("version"),
                currentApp.get("version_code"),
                currentApp.get("content"),
                "回滚前备份",
                currentApp.get("author_id"),
                currentApp.get("author_name")
            );
            
            // 回滚到目标版本
            String rollbackVersion = "v" + ((Number) targetVersion.get("version_code")).intValue() + ".0";
            jdbcTemplate.update(
                "UPDATE magic_sys_market_app SET content = ?, version = ?, version_code = ?, " +
                "update_content = ?, publish_time = CURRENT_TIMESTAMP WHERE id = ?",
                targetVersion.get("content"),
                rollbackVersion,
                ((Number) targetVersion.get("version_code")).intValue() + 1,
                "回滚到版本 " + targetVersion.get("version"),
                appId
            );
            
            result.put("code", 200);
            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("message", "已回滚到版本 " + targetVersion.get("version"));
            result.put("data", data);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取用户已发布的应用列表（带版本信息）
     */
    @GetMapping("/my-apps")
    public Map<String, Object> getMyApps(@RequestParam Integer userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> apps = jdbcTemplate.queryForList(
                "SELECT id, project_id, name, description, tags, likes, views, publish_time, version, version_code, update_content " +
                "FROM magic_sys_market_app WHERE author_id = ? AND status = 1 ORDER BY publish_time DESC",
                userId
            );
            
            // 解析 tags
            for (Map<String, Object> app : apps) {
                String tagsStr = (String) app.get("tags");
                if (tagsStr != null && !tagsStr.isEmpty()) {
                    app.put("tags", tagsStr.split(","));
                } else {
                    app.put("tags", new String[0]);
                }
            }
            
            result.put("code", 200);
            result.put("data", apps);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取项目对应的已发布应用信息
     */
    @GetMapping("/project/{projectId}/published")
    public Map<String, Object> getPublishedAppByProject(@PathVariable Integer projectId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> apps = jdbcTemplate.queryForList(
                "SELECT id, name, description, tags, likes, views, publish_time, version, version_code " +
                "FROM magic_sys_market_app WHERE project_id = ? AND status = 1 ORDER BY publish_time DESC LIMIT 1",
                projectId
            );
            
            if (apps.isEmpty()) {
                result.put("code", 404);
                result.put("message", "该项目尚未发布到市场");
            } else {
                Map<String, Object> app = apps.get(0);
                String tagsStr = (String) app.get("tags");
                if (tagsStr != null && !tagsStr.isEmpty()) {
                    app.put("tags", tagsStr.split(","));
                } else {
                    app.put("tags", new String[0]);
                }
                result.put("code", 200);
                result.put("data", app);
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 下架应用
     */
    @DeleteMapping("/apps/{id}")
    public Map<String, Object> unpublishApp(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        try {
            jdbcTemplate.update("UPDATE magic_sys_market_app SET status = 0 WHERE id = ?", id);
            result.put("code", 200);
            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            result.put("data", data);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 点赞/取消点赞
     */
    @PostMapping("/apps/{id}/like")
    public Map<String, Object> toggleLike(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer userId = body.get("userId") != null ? 
                Integer.parseInt(body.get("userId").toString()) : 1;
            
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM magic_sys_market_app_like WHERE app_id = ? AND user_id = ?",
                Integer.class, id, userId
            );
            
            if (count != null && count > 0) {
                jdbcTemplate.update(
                    "DELETE FROM magic_sys_market_app_like WHERE app_id = ? AND user_id = ?",
                    id, userId
                );
                jdbcTemplate.update(
                    "UPDATE magic_sys_market_app SET likes = likes - 1 WHERE id = ?",
                    id
                );
                Map<String, Object> likeData = new HashMap<>();
                likeData.put("liked", false);
                result.put("data", likeData);
            } else {
                jdbcTemplate.update(
                    "INSERT INTO magic_sys_market_app_like (app_id, user_id) VALUES (?, ?)",
                    id, userId
                );
                jdbcTemplate.update(
                    "UPDATE magic_sys_market_app SET likes = likes + 1 WHERE id = ?",
                    id
                );
                
                try {
                    List<Map<String, Object>> appInfo = jdbcTemplate.queryForList(
                        "SELECT author_id, name FROM magic_sys_market_app WHERE id = ?", id
                    );
                    if (!appInfo.isEmpty()) {
                        Integer authorId = (Integer) appInfo.get(0).get("author_id");
                        String appName = (String) appInfo.get(0).get("name");
                        if (authorId != null && !authorId.equals(userId)) {
                            jdbcTemplate.update(
                                "INSERT INTO magic_sys_notification (user_id, type, title, content, related_id, create_time, is_read) VALUES (?, ?, ?, ?, ?, NOW(), 0)",
                                authorId, "like", "您的应用收到点赞", "用户赞了您的应用「" + appName + "」", id
                            );
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                Map<String, Object> likeData = new HashMap<>();
                likeData.put("liked", true);
                result.put("data", likeData);
            }
            
            Integer likes = jdbcTemplate.queryForObject(
                "SELECT likes FROM magic_sys_market_app WHERE id = ?",
                Integer.class, id
            );
            
            result.put("code", 200);
            Map<String, Object> likeResultData = new HashMap<>();
            likeResultData.put("success", true);
            likeResultData.put("liked", count == null || count == 0);
            likeResultData.put("likes", likes != null ? likes : 0);
            result.put("data", likeResultData);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 版本号自增
     */
    private String incrementVersion(String currentVersion, String type) {
        if (currentVersion == null || currentVersion.isEmpty()) {
            return "1.0.0";
        }
        
        String[] parts = currentVersion.split("\\.");
        int major = 0, minor = 0, patch = 0;
        
        try {
            if (parts.length >= 1) major = Integer.parseInt(parts[0]);
            if (parts.length >= 2) minor = Integer.parseInt(parts[1]);
            if (parts.length >= 3) patch = Integer.parseInt(parts[2].replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            patch = 1;
        }
        
        switch (type) {
            case "major":
                major++;
                minor = 0;
                patch = 0;
                break;
            case "minor":
                minor++;
                patch = 0;
                break;
            case "patch":
            default:
                patch++;
                break;
        }
        
        return major + "." + minor + "." + patch;
    }

    // ==================== 社区功能 API ====================

    /**
     * 获取应用评论列表
     */
    @GetMapping("/apps/{id}/comments")
    public Map<String, Object> getAppComments(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> comments = jdbcTemplate.queryForList(
                "SELECT * FROM magic_sys_market_app_comment WHERE app_id = ? AND parent_id = 0 ORDER BY create_time DESC",
                id
            );
            
            for (Map<String, Object> comment : comments) {
                List<Map<String, Object>> replies = jdbcTemplate.queryForList(
                    "SELECT * FROM magic_sys_market_app_comment WHERE parent_id = ? ORDER BY create_time ASC",
                    comment.get("id")
                );
                comment.put("replies", replies);
            }
            
            Map<String, Object> stats = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) as total, AVG(rating) as avg_rating FROM magic_sys_market_app_comment WHERE app_id = ?",
                id
            );
            
            result.put("code", 200);
            result.put("data", comments);
            result.put("stats", stats);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 添加评论
     */
    @PostMapping("/apps/{id}/comments")
    public Map<String, Object> addComment(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer userId = body.get("userId") != null ? 
                Integer.parseInt(body.get("userId").toString()) : 1;
            String userName = (String) body.get("userName");
            if (userName == null || userName.isEmpty()) userName = "匿名用户";
            String userAvatar = (String) body.get("userAvatar");
            String content = (String) body.get("content");
            Integer rating = body.get("rating") != null ? 
                Integer.parseInt(body.get("rating").toString()) : 5;
            Integer parentId = body.get("parentId") != null ? 
                Integer.parseInt(body.get("parentId").toString()) : 0;
            
            jdbcTemplate.update(
                "INSERT INTO magic_sys_market_app_comment (app_id, user_id, user_name, user_avatar, content, rating, parent_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                id, userId, userName, userAvatar, content, rating, parentId
            );
            
            try {
                List<Map<String, Object>> appInfo = jdbcTemplate.queryForList(
                    "SELECT author_id, name FROM magic_sys_market_app WHERE id = ?", id
                );
                if (!appInfo.isEmpty()) {
                    Integer authorId = (Integer) appInfo.get(0).get("author_id");
                    String appName = (String) appInfo.get(0).get("name");
                    if (authorId != null && !authorId.equals(userId)) {
                        if (parentId != null && parentId > 0) {
                            List<Map<String, Object>> parentComment = jdbcTemplate.queryForList(
                                "SELECT user_id, user_name FROM magic_sys_market_app_comment WHERE id = ?", parentId
                            );
                            if (!parentComment.isEmpty()) {
                                Integer replyUserId = (Integer) parentComment.get(0).get("user_id");
                                String replyUserName = (String) parentComment.get(0).get("user_name");
                                jdbcTemplate.update(
                                    "INSERT INTO magic_sys_notification (user_id, type, title, content, related_id, create_time, is_read) VALUES (?, ?, ?, ?, ?, NOW(), 0)",
                                    replyUserId, "reply", "收到回复", userName + "回复了您的评论", id
                                );
                            }
                        } else {
                            jdbcTemplate.update(
                                "INSERT INTO magic_sys_notification (user_id, type, title, content, related_id, create_time, is_read) VALUES (?, ?, ?, ?, ?, NOW(), 0)",
                                authorId, "comment", "收到评论", userName + "评论了您的应用「" + appName + "」", id
                            );
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            result.put("code", 200);
            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("message", "评论成功");
            result.put("data", data);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/comments/{commentId}")
    public Map<String, Object> deleteComment(@PathVariable Integer commentId, @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer userId = body.get("userId") != null ? 
                Integer.parseInt(body.get("userId").toString()) : 1;
            
            jdbcTemplate.update(
                "DELETE FROM magic_sys_market_app_comment WHERE id = ? AND user_id = ?",
                commentId, userId
            );
            
            result.put("code", 200);
            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("message", "删除成功");
            result.put("data", data);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 收藏/取消收藏应用
     */
    @PostMapping("/apps/{id}/favorite")
    public Map<String, Object> toggleFavorite(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer userId = body.get("userId") != null ? 
                Integer.parseInt(body.get("userId").toString()) : 1;
            
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM magic_sys_market_app_favorite WHERE app_id = ? AND user_id = ?",
                Integer.class, id, userId
            );
            
            if (count != null && count > 0) {
                jdbcTemplate.update(
                    "DELETE FROM magic_sys_market_app_favorite WHERE app_id = ? AND user_id = ?",
                    id, userId
                );
                Map<String, Object> data = new HashMap<>();
                data.put("favorited", false);
                data.put("message", "取消收藏成功");
                result.put("data", data);
            } else {
                jdbcTemplate.update(
                    "INSERT INTO magic_sys_market_app_favorite (app_id, user_id) VALUES (?, ?)",
                    id, userId
                );
                Map<String, Object> data = new HashMap<>();
                data.put("favorited", true);
                data.put("message", "收藏成功");
                result.put("data", data);
            }
            
            result.put("code", 200);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取用户收藏列表
     */
    @GetMapping("/favorites")
    public Map<String, Object> getUserFavorites(@RequestParam Integer userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> favorites = jdbcTemplate.queryForList(
                "SELECT f.*, m.name as app_name, m.description, m.thumbnail, m.author_name, m.likes, m.views " +
                "FROM magic_sys_market_app_favorite f " +
                "LEFT JOIN magic_sys_market_app m ON f.app_id = m.id " +
                "WHERE f.user_id = ? ORDER BY f.create_time DESC",
                userId
            );
            
            result.put("code", 200);
            result.put("data", favorites);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 检查用户是否收藏了应用
     */
    @GetMapping("/apps/{id}/favorite/check")
    public Map<String, Object> checkFavorite(@PathVariable Integer id, @RequestParam Integer userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM magic_sys_market_app_favorite WHERE app_id = ? AND user_id = ?",
                Integer.class, id, userId
            );
            
            result.put("code", 200);
            result.put("data", Collections.singletonMap("favorited", count != null && count > 0));
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 关注/取消关注用户
     */
    @PostMapping("/users/{userId}/follow")
    public Map<String, Object> toggleFollow(@PathVariable Integer userId, @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer followerId = body.get("followerId") != null ? 
                Integer.parseInt(body.get("followerId").toString()) : 1;
            
            if (followerId.equals(userId)) {
                result.put("code", 400);
                result.put("message", "不能关注自己");
                return result;
            }
            
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM magic_sys_user_follow WHERE follower_id = ? AND followee_id = ?",
                Integer.class, followerId, userId
            );
            
            if (count != null && count > 0) {
                jdbcTemplate.update(
                    "DELETE FROM magic_sys_user_follow WHERE follower_id = ? AND followee_id = ?",
                    followerId, userId
                );
                Map<String, Object> data = new HashMap<>();
                data.put("following", false);
                data.put("message", "取消关注成功");
                result.put("data", data);
            } else {
                jdbcTemplate.update(
                    "INSERT INTO magic_sys_user_follow (follower_id, followee_id) VALUES (?, ?)",
                    followerId, userId
                );
                Map<String, Object> data = new HashMap<>();
                data.put("following", true);
                data.put("message", "关注成功");
                result.put("data", data);
            }
            
            result.put("code", 200);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取用户粉丝列表
     */
    @GetMapping("/users/{userId}/followers")
    public Map<String, Object> getUserFollowers(@PathVariable Integer userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> followers = jdbcTemplate.queryForList(
                "SELECT u.id, u.username, u.avatar, u.bio, f.create_time as follow_time " +
                "FROM magic_sys_user_follow f " +
                "LEFT JOIN magic_sys_user u ON f.follower_id = u.id " +
                "WHERE f.followee_id = ? ORDER BY f.create_time DESC",
                userId
            );
            
            result.put("code", 200);
            result.put("data", followers);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取用户关注列表
     */
    @GetMapping("/users/{userId}/following")
    public Map<String, Object> getUserFollowing(@PathVariable Integer userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> following = jdbcTemplate.queryForList(
                "SELECT u.id, u.username, u.avatar, u.bio, f.create_time as follow_time " +
                "FROM magic_sys_user_follow f " +
                "LEFT JOIN magic_sys_user u ON f.followee_id = u.id " +
                "WHERE f.follower_id = ? ORDER BY f.create_time DESC",
                userId
            );
            
            result.put("code", 200);
            result.put("data", following);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 检查是否关注了用户
     */
    @GetMapping("/users/{userId}/follow/check")
    public Map<String, Object> checkFollow(@PathVariable Integer userId, @RequestParam Integer followerId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM magic_sys_user_follow WHERE follower_id = ? AND followee_id = ?",
                Integer.class, followerId, userId
            );
            
            result.put("code", 200);
            result.put("data", Collections.singletonMap("following", count != null && count > 0));
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取用户发布的应用列表
     */
    @GetMapping("/users/{userId}/apps")
    public Map<String, Object> getUserApps(@PathVariable Integer userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> apps = jdbcTemplate.queryForList(
                "SELECT id, name, description, thumbnail, likes, views, publish_time, version " +
                "FROM magic_sys_market_app WHERE author_id = ? AND status = 1 ORDER BY publish_time DESC",
                userId
            );
            
            result.put("code", 200);
            result.put("data", apps);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    // ==================== 模板市场 API ====================

    /**
     * 获取模板列表
     */
    @GetMapping("/templates")
    public Map<String, Object> getTemplates(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        try {
            StringBuilder sql = new StringBuilder(
                "SELECT * FROM magic_sys_template WHERE status = 1"
            );
            List<Object> params = new ArrayList<>();
            
            // 类型筛选（official: 官方模板, user: 用户模板）
            if (type != null && !type.isEmpty()) {
                sql.append(" AND type = ?");
                params.add(type);
            }
            
            // 分类筛选
            if (category != null && !category.isEmpty()) {
                sql.append(" AND category = ?");
                params.add(category);
            }
            
            sql.append(" ORDER BY sort_order ASC, create_time DESC");
            
            // 获取总数
            String countSql = sql.toString().replace(
                "SELECT *", "SELECT COUNT(*)"
            );
            Integer total = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());
            
            // 分页
            sql.append(" LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add((page - 1) * pageSize);
            
            List<Map<String, Object>> templates = jdbcTemplate.queryForList(sql.toString(), params.toArray());
            
            result.put("code", 200);
            Map<String, Object> data = new HashMap<>();
            data.put("list", templates);
            data.put("total", total != null ? total : 0);
            result.put("data", data);
        } catch (Exception e) {
            // 如果表不存在，返回默认模板
            result.put("code", 200);
            Map<String, Object> data = new HashMap<>();
            data.put("list", getDefaultTemplates());
            data.put("total", 5);
            result.put("data", data);
        }
        return result;
    }

    /**
     * 获取默认模板列表
     */
    private List<Map<String, Object>> getDefaultTemplates() {
        List<Map<String, Object>> templates = new ArrayList<>();
        
        Map<String, Object> t1 = new HashMap<>();
        t1.put("id", 1);
        t1.put("name", "空白项目");
        t1.put("description", "从零开始的空白Vue项目");
        t1.put("category", "基础");
        t1.put("type", "official");
        t1.put("thumbnail", "");
        t1.put("content", "{}");
        t1.put("likes", 100);
        t1.put("usage_count", 500);
        templates.add(t1);
        
        Map<String, Object> t2 = new HashMap<>();
        t2.put("id", 2);
        t2.put("name", "待办事项");
        t2.put("description", "完整的待办事项应用，支持添加、删除、标记完成");
        t2.put("category", "工具");
        t2.put("type", "official");
        t2.put("thumbnail", "");
        t2.put("content", "{}");
        t2.put("likes", 200);
        t2.put("usage_count", 800);
        templates.add(t2);
        
        Map<String, Object> t3 = new HashMap<>();
        t3.put("id", 3);
        t3.put("name", "计算器");
        t3.put("description", "功能完善的计算器应用");
        t3.put("category", "工具");
        t3.put("type", "official");
        t3.put("thumbnail", "");
        t3.put("content", "{}");
        t3.put("likes", 150);
        t3.put("usage_count", 600);
        templates.add(t3);
        
        Map<String, Object> t4 = new HashMap<>();
        t4.put("id", 4);
        t4.put("name", "天气预报");
        t4.put("description", "展示天气信息的单页应用");
        t4.put("category", "展示");
        t4.put("type", "official");
        t4.put("thumbnail", "");
        t4.put("content", "{}");
        t4.put("likes", 180);
        t4.put("usage_count", 700);
        templates.add(t4);
        
        Map<String, Object> t5 = new HashMap<>();
        t5.put("id", 5);
        t5.put("name", "贪吃蛇游戏");
        t5.put("description", "经典贪吃蛇小游戏");
        t5.put("category", "游戏");
        t5.put("type", "official");
        t5.put("thumbnail", "");
        t5.put("content", "{}");
        t5.put("likes", 250);
        t5.put("usage_count", 900);
        templates.add(t5);
        
        return templates;
    }

    /**
     * 获取模板详情
     */
    @GetMapping("/templates/{id}")
    public Map<String, Object> getTemplateDetail(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> templates = jdbcTemplate.queryForList(
                "SELECT * FROM magic_sys_template WHERE id = ? AND status = 1",
                id
            );
            
            if (templates.isEmpty()) {
                // 返回默认模板
                List<Map<String, Object>> defaults = getDefaultTemplates();
                for (Map<String, Object> t : defaults) {
                    if (id.equals(t.get("id"))) {
                        result.put("code", 200);
                        result.put("data", t);
                        return result;
                    }
                }
                result.put("code", 404);
                result.put("message", "模板不存在");
            } else {
                result.put("code", 200);
                result.put("data", templates.get(0));
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 使用模板创建项目
     */
    @PostMapping("/templates/{id}/use")
    public Map<String, Object> useTemplate(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer userId = body.get("userId") != null ? 
                Integer.parseInt(body.get("userId").toString()) : 1;
            String projectName = (String) body.get("projectName");
            if (projectName == null || projectName.isEmpty()) {
                projectName = "新项目";
            }
            
            Map<String, Object> template = null;
            try {
                List<Map<String, Object>> templates = jdbcTemplate.queryForList(
                    "SELECT * FROM magic_sys_template WHERE id = ?", id
                );
                if (!templates.isEmpty()) {
                    template = templates.get(0);
                }
            } catch (Exception e) {
                // 表可能不存在
            }
            
            // 如果没有找到模板，使用默认模板
            if (template == null) {
                List<Map<String, Object>> defaults = getDefaultTemplates();
                for (Map<String, Object> t : defaults) {
                    if (id.equals(t.get("id"))) {
                        template = t;
                        break;
                    }
                }
            }
            
            if (template == null) {
                result.put("code", 404);
                result.put("message", "模板不存在");
                return result;
            }
            
            // 创建项目
            String content = (String) template.get("content");
            jdbcTemplate.update(
                "INSERT INTO magic_sys_project (name, description, owner_id, content, create_time, update_time) " +
                "VALUES (?, ?, ?, ?, datetime('now'), datetime('now'))",
                projectName, template.get("description"), userId, content
            );
            
            Integer projectId = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Integer.class);
            
            // 增加模板使用次数
            try {
                jdbcTemplate.update(
                    "UPDATE magic_sys_template SET usage_count = usage_count + 1 WHERE id = ?",
                    id
                );
            } catch (Exception e) {
                // 忽略
            }
            
            result.put("code", 200);
            Map<String, Object> data = new HashMap<>();
            data.put("projectId", projectId);
            data.put("message", "项目创建成功");
            result.put("data", data);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    // ==================== 用户活动日志 API ====================

    /**
     * 记录用户活动
     */
    @PostMapping("/activity/log")
    public Map<String, Object> logActivity(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer userId = body.get("userId") != null ? 
                Integer.parseInt(body.get("userId").toString()) : 1;
            String activityType = (String) body.get("type");
            String content = (String) body.get("content");
            Integer relatedId = body.get("relatedId") != null ? 
                Integer.parseInt(body.get("relatedId").toString()) : null;
            
            jdbcTemplate.update(
                "INSERT INTO magic_sys_user_activity (user_id, type, content, related_id, create_time) " +
                "VALUES (?, ?, ?, ?, datetime('now'))",
                userId, activityType, content, relatedId
            );
            
            result.put("code", 200);
            result.put("data", Collections.singletonMap("success", true));
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取用户活动日志
     */
    @GetMapping("/activity/logs")
    public Map<String, Object> getActivityLogs(
            @RequestParam Integer userId,
            @RequestParam(defaultValue = "20") Integer limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> activities = jdbcTemplate.queryForList(
                "SELECT * FROM magic_sys_user_activity WHERE user_id = ? ORDER BY create_time DESC LIMIT ?",
                userId, limit
            );
            
            result.put("code", 200);
            result.put("data", activities);
        } catch (Exception e) {
            result.put("code", 200);
            result.put("data", new ArrayList<>());
        }
        return result;
    }

    /**
     * 获取应用评分权重（综合评分）
     * 算法：基于评分、点赞数、浏览量、发布时间计算综合得分
     */
    @GetMapping("/apps/{id}/score")
    public Map<String, Object> getAppScore(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 获取应用基本信息
            List<Map<String, Object>> apps = jdbcTemplate.queryForList(
                "SELECT likes, views, publish_time, version FROM magic_sys_market_app WHERE id = ? AND status = 1",
                id
            );
            
            if (apps.isEmpty()) {
                result.put("code", 404);
                result.put("message", "应用不存在");
                return result;
            }
            
            Map<String, Object> app = apps.get(0);
            Integer likes = app.get("likes") != null ? ((Number) app.get("likes")).intValue() : 0;
            Integer views = app.get("views") != null ? ((Number) app.get("views")).intValue() : 0;
            String publishTime = (String) app.get("publish_time");
            
            // 获取评分统计
            Map<String, Object> ratingStats = new HashMap<>();
            try {
                List<Map<String, Object>> stats = jdbcTemplate.queryForList(
                    "SELECT COUNT(*) as total, AVG(rating) as avg_rating, " +
                    "SUM(CASE WHEN rating = 5 THEN 1 ELSE 0 END) as star5, " +
                    "SUM(CASE WHEN rating = 4 THEN 1 ELSE 0 END) as star4, " +
                    "SUM(CASE WHEN rating = 3 THEN 1 ELSE 0 END) as star3, " +
                    "SUM(CASE WHEN rating = 2 THEN 1 ELSE 0 END) as star2, " +
                    "SUM(CASE WHEN rating = 1 THEN 1 ELSE 0 END) as star1 " +
                    "FROM magic_sys_market_app_comment WHERE app_id = ?",
                    id
                );
                if (!stats.isEmpty()) {
                    ratingStats = stats.get(0);
                }
            } catch (Exception e) {
                ratingStats.put("total", 0);
                ratingStats.put("avg_rating", 0.0);
            }
            
            Integer totalRatings = ratingStats.get("total") != null ? 
                ((Number) ratingStats.get("total")).intValue() : 0;
            Double avgRating = ratingStats.get("avg_rating") != null ? 
                ((Number) ratingStats.get("avg_rating")).doubleValue() : 0.0;
            
            // 计算综合得分
            double score = calculateAppScore(likes, views, totalRatings, avgRating, publishTime);
            
            // 分解得分
            double ratingScore = avgRating * 20; // 评分得分（满分20）
            double likeScore = Math.min(likes * 0.3, 30); // 点赞得分（满分30）
            double viewScore = Math.min(views * 0.01, 20); // 浏览得分（满分20）
            double recencyScore = calculateRecencyScore(publishTime); // 时效性得分（满分30）
            
            Map<String, Object> data = new HashMap<>();
            data.put("totalScore", Math.round(score * 100) / 100.0);
            data.put("ratingScore", Math.round(ratingScore * 100) / 100.0);
            data.put("likeScore", Math.round(likeScore * 100) / 100.0);
            data.put("viewScore", Math.round(viewScore * 100) / 100.0);
            data.put("recencyScore", Math.round(recencyScore * 100) / 100.0);
            data.put("avgRating", Math.round(avgRating * 10) / 10.0);
            data.put("totalRatings", totalRatings);
            data.put("likes", likes);
            data.put("views", views);
            
            result.put("code", 200);
            result.put("data", data);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 计算应用综合得分
     */
    private double calculateAppScore(int likes, int views, int totalRatings, double avgRating, String publishTime) {
        double score = 0;
        
        // 评分得分 (40%)
        score += avgRating * 8;
        
        // 点赞得分 (30%)
        score += Math.min(likes * 0.3, 30);
        
        // 浏览量得分 (10%)
        score += Math.min(views * 0.01, 10);
        
        // 时效性得分 (20%)
        score += calculateRecencyScore(publishTime);
        
        return score;
    }

    /**
     * 计算时效性得分
     */
    private double calculateRecencyScore(String publishTime) {
        if (publishTime == null || publishTime.isEmpty()) {
            return 15.0;
        }
        
        try {
            // 简单计算：假设publishTime格式为 YYYY-MM-DD HH:MM:SS
            java.time.LocalDateTime publishDate = java.time.LocalDateTime.parse(publishTime.replace(" ", "T"));
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            long daysBetween = java.time.Duration.between(publishDate, now).toDays();
            
            // 30天内得分30，之后每30天减5分，最低0分
            if (daysBetween <= 30) {
                return 30.0;
            } else if (daysBetween <= 60) {
                return 25.0;
            } else if (daysBetween <= 90) {
                return 20.0;
            } else if (daysBetween <= 180) {
                return 15.0;
            } else if (daysBetween <= 365) {
                return 10.0;
            } else {
                return 5.0;
            }
        } catch (Exception e) {
            return 15.0;
        }
    }
}

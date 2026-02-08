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
     * 获取应用列表
     */
    @GetMapping("/apps")
    public Map<String, Object> getApps(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
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
            
            // 关键词搜索
            if (keyword != null && !keyword.isEmpty()) {
                sql.append(" AND (name LIKE ? OR description LIKE ?)");
                params.add("%" + keyword + "%");
                params.add("%" + keyword + "%");
            }
            
            // 分类筛选
            if (category != null && !category.isEmpty()) {
                sql.append(" AND tags LIKE ?");
                params.add("%" + category + "%");
            }
            
            // 获取总数
            String countSql = sql.toString().replace(
                "SELECT id, project_id, name, description, tags, thumbnail, author_id, author_name, author_avatar, likes, views, publish_time, is_open_source, version",
                "SELECT COUNT(*)"
            );
            Integer total = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());
            
            // 分页查询
            sql.append(" ORDER BY publish_time DESC LIMIT ? OFFSET ?");
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
                result.put("data", Map.of("favorited", false, "message", "取消收藏成功"));
            } else {
                jdbcTemplate.update(
                    "INSERT INTO magic_sys_market_app_favorite (app_id, user_id) VALUES (?, ?)",
                    id, userId
                );
                result.put("data", Map.of("favorited", true, "message", "收藏成功"));
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
            result.put("data", Map.of("favorited", count != null && count > 0));
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
                result.put("data", Map.of("following", false, "message", "取消关注成功"));
            } else {
                jdbcTemplate.update(
                    "INSERT INTO magic_sys_user_follow (follower_id, followee_id) VALUES (?, ?)",
                    followerId, userId
                );
                result.put("data", Map.of("following", true, "message", "关注成功"));
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
            result.put("data", Map.of("following", count != null && count > 0));
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
}

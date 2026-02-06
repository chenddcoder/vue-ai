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
                "author_id, author_name, author_avatar, likes, views, publish_time " +
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
                "SELECT id, project_id, name, description, tags, thumbnail, author_id, author_name, author_avatar, likes, views, publish_time",
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
     * 发布应用到市场
     */
    @PostMapping("/publish")
    public Map<String, Object> publishApp(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer projectId = body.get("projectId") != null ? 
                Integer.parseInt(body.get("projectId").toString()) : null;
            String name = (String) body.get("name");
            String description = (String) body.get("description");
            String content = objectMapper.writeValueAsString(body.get("content"));
            
            // 获取作者信息（从请求头或session中获取，这里简化处理）
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
            
            jdbcTemplate.update(
                "INSERT INTO magic_sys_market_app (project_id, name, description, tags, content, author_id, author_name) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                projectId, name, description, tags, content, authorId, authorName
            );
            
            Integer appId = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Integer.class);
            
            result.put("code", 200);
            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("appId", appId);
            data.put("message", "发布成功");
            result.put("data", data);
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
            
            // 检查是否已点赞
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM magic_sys_market_app_like WHERE app_id = ? AND user_id = ?",
                Integer.class, id, userId
            );
            
            if (count != null && count > 0) {
                // 取消点赞
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
                // 添加点赞
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
            
            // 获取最新点赞数
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
     * 获取用户的应用列表（已发布）
     */
    @GetMapping("/my-apps")
    public Map<String, Object> getMyApps(@RequestParam Integer userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> apps = jdbcTemplate.queryForList(
                "SELECT id, project_id, name, description, tags, likes, views, publish_time " +
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
}

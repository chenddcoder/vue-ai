package com.vueai.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/notification")
@CrossOrigin
public class NotificationController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/list")
    public Map<String, Object> getNotifications(
            @RequestParam Integer userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        try {
            String sql = "SELECT * FROM magic_sys_notification WHERE user_id = ? ORDER BY create_time DESC LIMIT ? OFFSET ?";
            List<Map<String, Object>> notifications = jdbcTemplate.queryForList(sql, userId, pageSize, (page - 1) * pageSize);
            
            String countSql = "SELECT COUNT(*) FROM magic_sys_notification WHERE user_id = ?";
            Integer total = jdbcTemplate.queryForObject(countSql, Integer.class, userId);
            
            result.put("code", 200);
            Map<String, Object> data = new HashMap<>();
            data.put("list", notifications);
            data.put("total", total != null ? total : 0);
            result.put("data", data);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    @PutMapping("/read/{id}")
    public Map<String, Object> markRead(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        try {
            jdbcTemplate.update("UPDATE magic_sys_notification SET is_read = 1, read_time = NOW() WHERE id = ?", id);
            result.put("code", 200);
            result.put("message", "标记已读成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    @PutMapping("/read-all")
    public Map<String, Object> markAllRead(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer userId = (Integer) body.get("userId");
            jdbcTemplate.update("UPDATE magic_sys_notification SET is_read = 1, read_time = NOW() WHERE user_id = ?", userId);
            result.put("code", 200);
            result.put("message", "全部已读成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    @GetMapping("/unread-count")
    public Map<String, Object> getUnreadCount(@RequestParam Integer userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM magic_sys_notification WHERE user_id = ? AND is_read = 0",
                Integer.class, userId
            );
            result.put("code", 200);
            result.put("data", count != null ? count : 0);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteNotification(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        try {
            jdbcTemplate.update("DELETE FROM magic_sys_notification WHERE id = ?", id);
            result.put("code", 200);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    public void createNotification(Integer userId, String type, String title, String content, Integer relatedId) {
        try {
            jdbcTemplate.update(
                "INSERT INTO magic_sys_notification (user_id, type, title, content, related_id, create_time, is_read) VALUES (?, ?, ?, ?, ?, NOW(), 0)",
                userId, type, title, content, relatedId
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package com.vueai.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> users = jdbcTemplate.queryForList("SELECT * FROM magic_sys_user WHERE username = ?", username);
            if (users.isEmpty()) {
                result.put("code", 401);
                result.put("message", "User not found");
                return result;
            }
            
            Map<String, Object> user = users.get(0);
            if (!password.equals(user.get("password"))) {
                result.put("code", 401);
                result.put("message", "Invalid password");
                return result;
            }
            
            result.put("code", 200);
            result.put("message", "Login success");
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.get("id"));
            userData.put("username", user.get("username"));
            userData.put("avatar", user.get("avatar") != null ? user.get("avatar") : "");
            userData.put("bio", user.get("bio") != null ? user.get("bio") : "");
            userData.put("role", user.get("role"));
            result.put("data", userData);
            result.put("token", "token_" + user.get("id")); 
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        
        Map<String, Object> result = new HashMap<>();
        try {
            // 检查用户名是否已存在
            List<Map<String, Object>> existingUsers = jdbcTemplate.queryForList(
                "SELECT * FROM magic_sys_user WHERE username = ?", username);
            if (!existingUsers.isEmpty()) {
                result.put("code", 400);
                result.put("message", "Username already exists");
                return result;
            }
            
            // 创建新用户
            jdbcTemplate.update(
                "INSERT INTO magic_sys_user (username, password, role) VALUES (?, ?, ?)",
                username, password, "user");
            
            // 获取新创建的用户
            List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT * FROM magic_sys_user WHERE username = ?", username);
            Map<String, Object> user = users.get(0);
            
            result.put("code", 200);
            result.put("message", "Register success");
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.get("id"));
            userData.put("username", user.get("username"));
            userData.put("avatar", user.get("avatar") != null ? user.get("avatar") : "");
            userData.put("bio", user.get("bio") != null ? user.get("bio") : "");
            userData.put("role", user.get("role"));
            result.put("data", userData);
            result.put("token", "token_" + user.get("id"));
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/user")
    public Map<String, Object> getUserInfo(@RequestParam Integer userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT * FROM magic_sys_user WHERE id = ?", userId);
            if (users.isEmpty()) {
                result.put("code", 404);
                result.put("message", "User not found");
                return result;
            }
            
            Map<String, Object> user = users.get(0);
            result.put("code", 200);
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.get("id"));
            userData.put("username", user.get("username"));
            userData.put("avatar", user.get("avatar") != null ? user.get("avatar") : "");
            userData.put("bio", user.get("bio") != null ? user.get("bio") : "");
            userData.put("role", user.get("role"));
            result.put("data", userData);
            result.put("token", "token_" + user.get("id")); 
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @PostMapping("/avatar")
    public Map<String, Object> updateAvatar(@RequestBody Map<String, Object> body) {
        Integer userId = (Integer) body.get("userId");
        String avatar = (String) body.get("avatar");
        
        Map<String, Object> result = new HashMap<>();
        try {
            jdbcTemplate.update("UPDATE magic_sys_user SET avatar = ? WHERE id = ?", avatar, userId);
            
            List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT * FROM magic_sys_user WHERE id = ?", userId);
            if (!users.isEmpty()) {
                Map<String, Object> user = users.get(0);
                result.put("code", 200);
                result.put("message", "Avatar updated successfully");
                Map<String, Object> userData = new HashMap<>();
                userData.put("id", user.get("id"));
                userData.put("username", user.get("username"));
                userData.put("avatar", user.get("avatar") != null ? user.get("avatar") : "");
                userData.put("role", user.get("role"));
                result.put("data", userData);
            } else {
                result.put("code", 404);
                result.put("message", "User not found");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/password")
    public Map<String, Object> changePassword(@RequestBody Map<String, Object> body) {
        Integer userId = (Integer) body.get("userId");
        String oldPassword = (String) body.get("oldPassword");
        String newPassword = (String) body.get("newPassword");
        
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT * FROM magic_sys_user WHERE id = ?", userId);
            if (users.isEmpty()) {
                result.put("code", 404);
                result.put("message", "User not found");
                return result;
            }
            
            Map<String, Object> user = users.get(0);
            String currentPassword = (String) user.get("password");
            
            if (!oldPassword.equals(currentPassword)) {
                result.put("code", 401);
                result.put("message", "Current password is incorrect");
                return result;
            }
            
            if (newPassword == null || newPassword.length() < 6) {
                result.put("code", 400);
                result.put("message", "New password must be at least 6 characters");
                return result;
            }
            
            jdbcTemplate.update("UPDATE magic_sys_user SET password = ? WHERE id = ?", newPassword, userId);
            
            result.put("code", 200);
            result.put("message", "Password changed successfully");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
}

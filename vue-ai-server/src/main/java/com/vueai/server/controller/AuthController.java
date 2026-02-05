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
            result.put("data", user);
            result.put("token", "token_" + user.get("id")); 
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
}

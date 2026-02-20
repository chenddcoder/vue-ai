package com.vueai.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/share")
@CrossOrigin
public class ShareController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/record/{appId}")
    public Map<String, Object> recordShare(@PathVariable Integer appId, @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String shareType = (String) body.getOrDefault("shareType", "link");
            jdbcTemplate.update(
                "INSERT INTO magic_sys_market_app_share (app_id, share_type, share_time) VALUES (?, ?, NOW())",
                appId, shareType
            );
            result.put("code", 200);
            result.put("message", "分享记录成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    @GetMapping("/link/{appId}")
    public Map<String, Object> getShareLink(@PathVariable Integer appId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> apps = jdbcTemplate.queryForList(
                "SELECT id, name FROM magic_sys_market_app WHERE id = ?", appId
            );
            if (apps.isEmpty()) {
                result.put("code", 404);
                result.put("message", "应用不存在");
                return result;
            }
            result.put("code", 200);
            Map<String, Object> data = new HashMap<>();
            data.put("appId", appId);
            data.put("appName", apps.get(0).get("name"));
            result.put("data", data);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    @GetMapping("/stats/{appId}")
    public Map<String, Object> getShareStats(@PathVariable Integer appId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer totalShares = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM magic_sys_market_app_share WHERE app_id = ?",
                Integer.class, appId
            );
            result.put("code", 200);
            Map<String, Object> data = new HashMap<>();
            data.put("totalShares", totalShares != null ? totalShares : 0);
            result.put("data", data);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }
}

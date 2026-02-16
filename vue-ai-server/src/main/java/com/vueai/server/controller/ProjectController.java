package com.vueai.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/project")
@CrossOrigin
public class ProjectController {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Integer userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> projects = jdbcTemplate.queryForList("SELECT id, name, description, create_time FROM magic_sys_project WHERE owner_id = ?", userId);
            result.put("code", 200);
            result.put("data", projects);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> projects = jdbcTemplate.queryForList("SELECT * FROM magic_sys_project WHERE id = ?", id);
            if (projects.isEmpty()) {
                result.put("code", 404);
                result.put("message", "Project not found");
            } else {
                result.put("code", 200);
                result.put("data", projects.get(0));
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/save")
    public Map<String, Object> save(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer id = body.get("id") != null ? Integer.parseInt(body.get("id").toString()) : null;
            String name = (String) body.get("name");
            String description = (String) body.get("description");
            Integer ownerId = Integer.parseInt(body.get("ownerId").toString());
            String content = objectMapper.writeValueAsString(body.get("content"));

            if (id == null) {
                jdbcTemplate.update("INSERT INTO magic_sys_project (name, description, owner_id, content) VALUES (?, ?, ?, ?)",
                        name, description, ownerId, content);
                // 获取生成的ID
                Integer generatedId = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Integer.class);
                result.put("code", 200);
                result.put("message", "Saved successfully");
                Map<String, Object> data = new HashMap<String, Object>();
                data.put("id", generatedId);
                data.put("name", name);
                result.put("data", data);
            } else {
                jdbcTemplate.update("UPDATE magic_sys_project SET name = ?, description = ?, content = ? WHERE id = ?",
                        name, description, content, id);
                result.put("code", 200);
                result.put("message", "Updated successfully");
                Map<String, Object> data = new HashMap<String, Object>();
                data.put("id", id);
                data.put("name", name);
                result.put("data", data);
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        try {
            jdbcTemplate.update("DELETE FROM magic_sys_project WHERE id = ?", id);
            result.put("code", 200);
            result.put("message", "Deleted successfully");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    @PutMapping("/{id}/rename")
    public Map<String, Object> rename(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String name = (String) body.get("name");
            jdbcTemplate.update("UPDATE magic_sys_project SET name = ? WHERE id = ?", name, id);
            result.put("code", 200);
            result.put("message", "Renamed successfully");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }
}

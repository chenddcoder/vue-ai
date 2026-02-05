package com.vueai.server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class InitDb {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        // Create magic_sys_user table
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS magic_sys_user (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT NOT NULL UNIQUE," +
                "password TEXT NOT NULL," +
                "role TEXT DEFAULT 'user'," +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");
        
        // Create magic_sys_project table
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS magic_sys_project (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "description TEXT," +
                "owner_id INTEGER," +
                "content TEXT," +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");
        
        // Create magic_sys_project_member table
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS magic_sys_project_member (" +
                "project_id INTEGER," +
                "user_id INTEGER," +
                "role TEXT," +
                "PRIMARY KEY (project_id, user_id)" +
                ")");

        // Create default admin user if not exists
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM magic_sys_user WHERE username = 'admin'", Integer.class);
            if (count != null && count == 0) {
                jdbcTemplate.execute("INSERT INTO magic_sys_user (username, password, role) VALUES ('admin', '123456', 'admin')");
            }
        } catch (Exception e) {
            System.err.println("Error init admin user: " + e.getMessage());
        }
    }
}

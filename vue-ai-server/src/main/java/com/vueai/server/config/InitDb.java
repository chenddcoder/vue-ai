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

        // Create magic_sys_market_app table for app market
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS magic_sys_market_app (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "project_id INTEGER," +
                "name TEXT NOT NULL," +
                "description TEXT," +
                "tags TEXT," +
                "thumbnail TEXT," +
                "content TEXT," +
                "author_id INTEGER," +
                "author_name TEXT," +
                "author_avatar TEXT," +
                "likes INTEGER DEFAULT 0," +
                "views INTEGER DEFAULT 0," +
                "publish_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "status INTEGER DEFAULT 1" +
                ")");

        // Create magic_sys_market_app_like table for user likes
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS magic_sys_market_app_like (" +
                "app_id INTEGER," +
                "user_id INTEGER," +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "PRIMARY KEY (app_id, user_id)" +
                ")");

        // Create magic_sys_ai_config table for AI configurations
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS magic_sys_ai_config (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER NOT NULL," +
                "provider_id TEXT NOT NULL," +
                "model_id TEXT NOT NULL," +
                "config TEXT NOT NULL," +
                "is_active INTEGER DEFAULT 0," +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (user_id) REFERENCES magic_sys_user(id) ON DELETE CASCADE," +
                "UNIQUE(user_id, provider_id, model_id)" +
                ")");

        // Create indexes for AI config table
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_ai_config_user_id ON magic_sys_ai_config(user_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_ai_config_active ON magic_sys_ai_config(user_id, is_active)");

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

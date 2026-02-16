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
                "avatar TEXT," +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");
        
        // Add avatar column if not exists (for existing database)
        try {
            jdbcTemplate.execute("ALTER TABLE magic_sys_user ADD COLUMN avatar TEXT");
        } catch (Exception e) {
            // Column might already exist
        }

        // Add bio column if not exists (for existing database)
        try {
            jdbcTemplate.execute("ALTER TABLE magic_sys_user ADD COLUMN bio TEXT");
        } catch (Exception e) {
            // Column might already exist
        }
        
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
                "status INTEGER DEFAULT 1," +
                "is_open_source INTEGER DEFAULT 1" +
                ")");
        
        // Add is_open_source column if not exists (for existing database)
        try {
            jdbcTemplate.execute("ALTER TABLE magic_sys_market_app ADD COLUMN is_open_source INTEGER DEFAULT 1");
        } catch (Exception e) {
            // Column might already exist
        }

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

        // Create magic_sys_project_commit table for Git-like version control
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS magic_sys_project_commit (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "project_id INTEGER NOT NULL," +
                "commit_hash TEXT NOT NULL UNIQUE," +
                "parent_hash TEXT," +
                "author_id INTEGER," +
                "author_name TEXT," +
                "message TEXT NOT NULL," +
                "content TEXT NOT NULL," +
                "branch TEXT DEFAULT 'main'," +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (project_id) REFERENCES magic_sys_project(id) ON DELETE CASCADE" +
                ")");

        // Create magic_sys_project_branch table for branch management
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS magic_sys_project_branch (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "project_id INTEGER NOT NULL," +
                "name TEXT NOT NULL," +
                "current_commit_hash TEXT," +
                "is_default INTEGER DEFAULT 0," +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "UNIQUE(project_id, name)," +
                "FOREIGN KEY (project_id) REFERENCES magic_sys_project(id) ON DELETE CASCADE" +
                ")");

        // Create indexes for commit and branch tables
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_commit_project_id ON magic_sys_project_commit(project_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_commit_hash ON magic_sys_project_commit(commit_hash)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_branch_project_id ON magic_sys_project_branch(project_id)");

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

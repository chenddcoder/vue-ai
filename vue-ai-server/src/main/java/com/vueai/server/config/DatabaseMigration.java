package com.vueai.server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigration implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        migrateDatabase();
    }

    private void migrateDatabase() {
        try {
            // 添加 version 字段
            addColumnIfNotExists("version", "VARCHAR(20) DEFAULT '1.0.0'");
            addColumnIfNotExists("version_code", "INTEGER DEFAULT 1");
            addColumnIfNotExists("update_content", "TEXT");
            
            // 创建版本历史表
            createVersionTableIfNotExists();
            
            System.out.println("Database migration completed successfully");
        } catch (Exception e) {
            System.err.println("Database migration failed: " + e.getMessage());
        }
    }

    private void addColumnIfNotExists(String columnName, String columnDef) {
        try {
            jdbcTemplate.queryForObject("SELECT " + columnName + " FROM magic_sys_market_app", String.class);
            System.out.println("Column '" + columnName + "' already exists, skipping");
        } catch (Exception e) {
            try {
                System.out.println("Adding column '" + columnName + "'...");
                jdbcTemplate.execute("ALTER TABLE magic_sys_market_app ADD COLUMN " + columnName + " " + columnDef);
                System.out.println("Column '" + columnName + "' added successfully");
            } catch (Exception ex) {
                if (ex.getMessage() != null && ex.getMessage().contains("duplicate column")) {
                    System.out.println("Column '" + columnName + "' already exists (concurrent migration)");
                } else {
                    System.out.println("Column '" + columnName + "' may already exist: " + ex.getMessage());
                }
            }
        }
    }

    private void createVersionTableIfNotExists() {
        try {
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM magic_sys_market_app_version", Integer.class);
            System.out.println("Version table already exists, skipping");
        } catch (Exception e) {
            try {
                System.out.println("Creating version history table...");
                jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS magic_sys_market_app_version (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "app_id INTEGER NOT NULL, " +
                    "version VARCHAR(20) NOT NULL, " +
                    "version_code INTEGER NOT NULL, " +
                    "content TEXT NOT NULL, " +
                    "description TEXT, " +
                    "author_id INTEGER, " +
                    "author_name TEXT, " +
                    "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
                );
                jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_app_version ON magic_sys_market_app_version(app_id)");
                System.out.println("Version history table created successfully");
            } catch (Exception ex) {
                System.out.println("Version table may already exist: " + ex.getMessage());
            }
        }
    }
}

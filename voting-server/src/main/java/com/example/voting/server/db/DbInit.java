// voting-server/src/main/java/com/example/voting/server/db/DbInit.java
package com.example.voting.server.db;

import com.example.voting.server.util.PasswordUtil;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.List;

public final class DbInit {
    private static final Logger log = LoggerFactory.getLogger(DbInit.class);

    private DbInit() {}

    public static void init(HikariDataSource ds) {
        createTables(ds);
        seedData(ds);
    }

    private static void createTables(HikariDataSource ds) {
        List<String> ddl = List.of(
                """
                CREATE TABLE IF NOT EXISTS users (
                  id INT PRIMARY KEY AUTO_INCREMENT,
                  username VARCHAR(32) NOT NULL UNIQUE,
                  password VARCHAR(64) NOT NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """,
                """
                CREATE TABLE IF NOT EXISTS polls (
                  id INT PRIMARY KEY AUTO_INCREMENT,
                  title VARCHAR(100) NOT NULL,
                  options_json VARCHAR(1000) NOT NULL,
                  is_open TINYINT NOT NULL DEFAULT 1
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """,
                """
                CREATE TABLE IF NOT EXISTS votes (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  poll_id INT NOT NULL,
                  username VARCHAR(32) NOT NULL,
                  option_index INT NOT NULL,
                  vote_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  INDEX idx_poll (poll_id),
                  INDEX idx_user_poll (username, poll_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """
        );

        try (Connection c = ds.getConnection()) {
            for (String sql : ddl) {
                try (Statement st = c.createStatement()) {
                    st.execute(sql);
                }
            }
            log.info("DB tables ensured.");
        } catch (SQLException e) {
            throw new RuntimeException("DB init failed", e);
        }
    }

    private static void seedData(HikariDataSource ds) {
        try (Connection c = ds.getConnection()) {
            // user seed
            if (count(c, "SELECT COUNT(*) FROM users") == 0) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO users(username,password) VALUES(?,?)")) {
                    ps.setString(1, "test");
                    ps.setString(2, PasswordUtil.sha256Hex("123456"));
                    ps.executeUpdate();
                }
                log.info("Seed user: test/123456");
            }

            // polls seed
            if (count(c, "SELECT COUNT(*) FROM polls") == 0) {
                insertPoll(c, "你更喜欢的语言？", "[\"Java\",\"Python\",\"C++\",\"JavaScript\"]");
                insertPoll(c, "你更常用的数据库？", "[\"MySQL\",\"PostgreSQL\",\"SQLite\",\"MongoDB\"]");
                log.info("Seed polls inserted.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB seed failed", e);
        }
    }

    private static long count(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static void insertPoll(Connection c, String title, String optionsJson) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO polls(title, options_json, is_open) VALUES(?,?,1)")) {
            ps.setString(1, title);
            ps.setString(2, optionsJson);
            ps.executeUpdate();
        }
    }
}

// voting-server/src/main/java/com/example/voting/server/dao/UserDao.java
package com.example.voting.server.dao;

import com.example.voting.server.util.PasswordUtil;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;

public class UserDao {
    private final HikariDataSource ds;

    public UserDao(HikariDataSource ds) {
        this.ds = ds;
    }

    public boolean register(String username, String rawPassword) throws SQLException {
        String sql = "INSERT INTO users(username,password) VALUES(?,?)";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.sha256Hex(rawPassword));
            return ps.executeUpdate() == 1;
        }
    }

    public boolean exists(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username=?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean verifyLogin(String username, String rawPassword) throws SQLException {
        String sql = "SELECT password FROM users WHERE username=?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                String stored = rs.getString(1);
                String input = PasswordUtil.sha256Hex(rawPassword);
                return stored.equalsIgnoreCase(input);
            }
        }
    }
}

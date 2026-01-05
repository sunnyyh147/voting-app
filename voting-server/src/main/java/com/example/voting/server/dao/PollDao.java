package com.example.voting.server.dao;

import com.example.voting.common.dto.PollSummary;
import com.example.voting.server.util.JsonUtil;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PollDao {
    private final HikariDataSource ds;

    public PollDao(HikariDataSource ds) {
        this.ds = ds;
    }

    public List<PollSummary> listPolls() throws SQLException {
        String sql = "SELECT id, title, is_open FROM polls ORDER BY id DESC";
        List<PollSummary> list = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new PollSummary(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getInt("is_open") == 1
                ));
            }
        }
        return list;
    }

    public PollRow getPoll(int pollId) throws SQLException {
        String sql = "SELECT id, title, options_json, is_open FROM polls WHERE id=?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, pollId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                PollRow row = new PollRow();
                row.id = rs.getInt("id");
                row.title = rs.getString("title");
                row.optionsJson = rs.getString("options_json");
                row.open = rs.getInt("is_open") == 1;
                return row;
            }
        }
    }

    public List<String> parseOptions(String optionsJson) {
        try {
            return JsonUtil.MAPPER.readValue(
                    optionsJson,
                    JsonUtil.MAPPER.getTypeFactory().constructCollectionType(List.class, String.class)
            );
        } catch (Exception e) {
            throw new RuntimeException("options_json parse failed", e);
        }
    }

    public String toOptionsJson(List<String> options) {
        try {
            return JsonUtil.MAPPER.writeValueAsString(options);
        } catch (Exception e) {
            throw new RuntimeException("options_json write failed", e);
        }
    }

    /** 发布投票（管理员） */
    public int createPoll(String title, String optionsJson) throws SQLException {
        String sql = "INSERT INTO polls(title, options_json, is_open) VALUES(?,?,1)";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setString(2, optionsJson);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** 删除投票（管理员）：会同时删除 votes（不提供任何“改票数”的能力） */
    public boolean deletePollWithVotes(int pollId) throws SQLException {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement delVotes = c.prepareStatement("DELETE FROM votes WHERE poll_id=?");
                 PreparedStatement delPoll = c.prepareStatement("DELETE FROM polls WHERE id=?")) {
                delVotes.setInt(1, pollId);
                delVotes.executeUpdate();

                delPoll.setInt(1, pollId);
                int n = delPoll.executeUpdate();

                c.commit();
                return n == 1;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public static class PollRow {
        public int id;
        public String title;
        public String optionsJson;
        public boolean open;
    }
}

// voting-server/src/main/java/com/example/voting/server/dao/VoteDao.java
package com.example.voting.server.dao;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VoteDao {
    private final HikariDataSource ds;

    public VoteDao(HikariDataSource ds) {
        this.ds = ds;
    }

    public Integer getUserVote(int pollId, String username) throws SQLException {
        String sql = "SELECT option_index FROM votes WHERE poll_id=? AND username=? LIMIT 1";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, pollId);
            ps.setString(2, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getInt(1);
            }
        }
    }

    /** 新增投票（C） */
    public boolean insertVote(int pollId, String username, int optionIndex) throws SQLException {
        String sql = "INSERT INTO votes(poll_id, username, option_index) VALUES(?,?,?)";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, pollId);
            ps.setString(2, username);
            ps.setInt(3, optionIndex);
            return ps.executeUpdate() == 1;
        }
    }

    /** 改票（U） */
    public boolean updateVote(int pollId, String username, int optionIndex) throws SQLException {
        String sql = "UPDATE votes SET option_index=?, vote_time=CURRENT_TIMESTAMP WHERE poll_id=? AND username=?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, optionIndex);
            ps.setInt(2, pollId);
            ps.setString(3, username);
            return ps.executeUpdate() == 1;
        }
    }

    /** 撤销（D） */
    public boolean deleteVote(int pollId, String username) throws SQLException {
        String sql = "DELETE FROM votes WHERE poll_id=? AND username=?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, pollId);
            ps.setString(2, username);
            return ps.executeUpdate() >= 1;
        }
    }

    /** 统计（R） */
    public List<Long> countByOption(int pollId, int optionSize) throws SQLException {
        List<Long> counts = new ArrayList<>();
        for (int i = 0; i < optionSize; i++) counts.add(0L);

        String sql = "SELECT option_index, COUNT(*) cnt FROM votes WHERE poll_id=? GROUP BY option_index";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, pollId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idx = rs.getInt("option_index");
                    long cnt = rs.getLong("cnt");
                    if (idx >= 0 && idx < counts.size()) counts.set(idx, cnt);
                }
            }
        }
        return counts;
    }
}

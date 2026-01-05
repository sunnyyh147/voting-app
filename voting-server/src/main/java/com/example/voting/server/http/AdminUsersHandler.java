package com.example.voting.server.http;

import com.example.voting.server.auth.AuthManager;
import com.example.voting.server.dao.UserDao;
import com.example.voting.server.util.HttpIO;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.SQLException;

public class AdminUsersHandler extends BaseHandler {
    private final UserDao userDao;

    public AdminUsersHandler(AuthManager auth, UserDao userDao) {
        super(auth);
        this.userDao = userDao;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String user = requireUser(ex);
        if (user == null) return;

        try {
            if (!userDao.isAdmin(user)) {
                HttpIO.fail(ex, 403, "无权限（仅管理员）");
                return;
            }

            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                HttpIO.fail(ex, 405, "Method Not Allowed");
                return;
            }

            HttpIO.ok(ex, userDao.listUsers());
        } catch (SQLException e) {
            HttpIO.fail(ex, 500, "数据库错误: " + e.getMessage());
        } catch (Exception e) {
            HttpIO.fail(ex, 500, "服务器错误: " + e.getMessage());
        }
    }
}

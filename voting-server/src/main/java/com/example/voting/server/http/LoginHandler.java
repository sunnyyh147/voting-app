package com.example.voting.server.http;

import com.example.voting.common.dto.*;
import com.example.voting.server.auth.AuthManager;
import com.example.voting.server.dao.UserDao;
import com.example.voting.server.util.HttpIO;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.SQLException;

public class LoginHandler extends BaseHandler {
    private final UserDao userDao;

    public LoginHandler(AuthManager auth, UserDao userDao) {
        super(auth);
        this.userDao = userDao;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();
            if ("POST".equalsIgnoreCase(method)) {
                LoginRequest req = HttpIO.readJson(ex, LoginRequest.class);
                if (req.username == null || req.username.isBlank() || req.password == null || req.password.isBlank()) {
                    HttpIO.fail(ex, 400, "username/password 不能为空");
                    return;
                }

                String u = req.username.trim();
                boolean ok = userDao.verifyLogin(u, req.password);
                if (!ok) {
                    HttpIO.fail(ex, 403, "用户名或密码错误");
                    return;
                }

                boolean admin = userDao.isAdmin(u);
                String token = auth.issueToken(u);

                HttpIO.writeJson(ex, 200, ApiResponse.ok(new LoginResponseData(token, u, admin)));
                return;
            }
            HttpIO.fail(ex, 405, "Method Not Allowed");
        } catch (SQLException e) {
            HttpIO.fail(ex, 500, "数据库错误: " + e.getMessage());
        } catch (Exception e) {
            HttpIO.fail(ex, 500, "服务器错误: " + e.getMessage());
        }
    }
}

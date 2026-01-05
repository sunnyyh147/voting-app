// voting-server/src/main/java/com/example/voting/server/http/RegisterHandler.java
package com.example.voting.server.http;

import com.example.voting.common.dto.*;
import com.example.voting.server.auth.AuthManager;
import com.example.voting.server.dao.UserDao;
import com.example.voting.server.util.HttpIO;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.SQLException;

public class RegisterHandler extends BaseHandler {
    private final UserDao userDao;

    public RegisterHandler(AuthManager auth, UserDao userDao) {
        super(auth);
        this.userDao = userDao;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                HttpIO.fail(ex, 405, "Method Not Allowed");
                return;
            }
            RegisterRequest req = HttpIO.readJson(ex, RegisterRequest.class);
            if (req.username == null || req.username.isBlank() || req.password == null || req.password.isBlank()) {
                HttpIO.fail(ex, 400, "username/password 不能为空");
                return;
            }
            String u = req.username.trim();
            if (u.length() > 32) {
                HttpIO.fail(ex, 400, "用户名过长");
                return;
            }
            if (userDao.exists(u)) {
                HttpIO.fail(ex, 409, "用户名已存在");
                return;
            }
            userDao.register(u, req.password);
            HttpIO.writeJson(ex, 200, ApiResponse.okMsg("注册成功，请登录", null));
        } catch (SQLException e) {
            HttpIO.fail(ex, 500, "数据库错误: " + e.getMessage());
        } catch (Exception e) {
            HttpIO.fail(ex, 500, "服务器错误: " + e.getMessage());
        }
    }
}

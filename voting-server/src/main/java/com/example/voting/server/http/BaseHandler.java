// voting-server/src/main/java/com/example/voting/server/http/BaseHandler.java
package com.example.voting.server.http;

import com.example.voting.server.auth.AuthManager;
import com.example.voting.server.util.HttpIO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public abstract class BaseHandler implements HttpHandler {
    protected final AuthManager auth;

    protected BaseHandler(AuthManager auth) {
        this.auth = auth;
    }

    protected String requireUser(HttpExchange ex) throws IOException {
        String token = ex.getRequestHeaders().getFirst("X-Auth-Token");
        String user = auth.verifyToken(token);
        if (user == null) {
            HttpIO.fail(ex, 401, "未登录或登录已过期");
        }
        return user;
    }
}

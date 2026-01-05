// voting-server/src/main/java/com/example/voting/server/auth/AuthManager.java
package com.example.voting.server.auth;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthManager {
    private static final long EXPIRE_MS = 2L * 60 * 60 * 1000; // 2小时

    private final ConcurrentHashMap<String, Session> tokenMap = new ConcurrentHashMap<>();

    public String issueToken(String username) {
        String token = UUID.randomUUID().toString();
        tokenMap.put(token, new Session(username, System.currentTimeMillis()));
        return token;
    }

    public String verifyToken(String token) {
        if (token == null || token.isBlank()) return null;
        Session s = tokenMap.get(token);
        if (s == null) return null;
        if (System.currentTimeMillis() - s.issuedAt > EXPIRE_MS) {
            tokenMap.remove(token);
            return null;
        }
        return s.username;
    }

    private record Session(String username, long issuedAt) {}
}

// voting-server/src/main/java/com/example/voting/server/util/PasswordUtil.java
package com.example.voting.server.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class PasswordUtil {
    private PasswordUtil() {}

    public static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : dig) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("hash failed", e);
        }
    }
}

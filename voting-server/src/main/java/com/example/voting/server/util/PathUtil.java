// voting-server/src/main/java/com/example/voting/server/util/PathUtil.java
package com.example.voting.server.util;

public final class PathUtil {
    private PathUtil() {}

    public static String[] segments(String path) {
        if (path == null || path.isBlank()) return new String[0];
        String p = path.startsWith("/") ? path.substring(1) : path;
        if (p.endsWith("/")) p = p.substring(0, p.length() - 1);
        return p.isBlank() ? new String[0] : p.split("/");
    }

    public static Integer tryParseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return null; }
    }
}

// voting-server/src/main/java/com/example/voting/server/util/HttpIO.java
package com.example.voting.server.util;

import com.example.voting.common.dto.ApiResponse;
import com.fasterxml.jackson.databind.JavaType;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class HttpIO {
    private HttpIO() {}

    public static String readBodyAsString(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static <T> T readJson(HttpExchange ex, Class<T> clazz) throws IOException {
        String body = readBodyAsString(ex);
        return JsonUtil.MAPPER.readValue(body, clazz);
    }

    public static void writeJson(HttpExchange ex, int status, Object obj) throws IOException {
        byte[] bytes = JsonUtil.MAPPER.writeValueAsBytes(obj);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void ok(HttpExchange ex, Object data) throws IOException {
        writeJson(ex, 200, ApiResponse.ok(data));
    }

    public static void fail(HttpExchange ex, int status, String msg) throws IOException {
        writeJson(ex, status, ApiResponse.fail(msg));
    }
}

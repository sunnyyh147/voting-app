// voting-common/src/main/java/com/example/voting/common/dto/ApiResponse.java
package com.example.voting.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    public boolean success;
    public String message;
    public T data;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = true;
        r.data = data;
        return r;
    }

    public static <T> ApiResponse<T> okMsg(String msg, T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = true;
        r.message = msg;
        r.data = data;
        return r;
    }

    public static <T> ApiResponse<T> fail(String msg) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = false;
        r.message = msg;
        return r;
    }
}

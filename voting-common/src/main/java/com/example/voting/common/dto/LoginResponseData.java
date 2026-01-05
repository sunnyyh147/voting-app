// voting-common/src/main/java/com/example/voting/common/dto/LoginResponseData.java
package com.example.voting.common.dto;

public class LoginResponseData {
    public String token;
    public String username;

    public LoginResponseData() {}
    public LoginResponseData(String token, String username) {
        this.token = token;
        this.username = username;
    }
}

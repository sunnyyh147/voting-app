package com.example.voting.common.dto;

public class LoginResponseData {
    public String token;
    public String username;
    public boolean admin;

    public LoginResponseData() {}

    public LoginResponseData(String token, String username, boolean admin) {
        this.token = token;
        this.username = username;
        this.admin = admin;
    }
}

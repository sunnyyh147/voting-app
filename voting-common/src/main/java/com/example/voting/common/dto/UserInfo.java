package com.example.voting.common.dto;

public class UserInfo {
    public int id;
    public String username;
    public boolean admin;

    public UserInfo() {}
    public UserInfo(int id, String username, boolean admin) {
        this.id = id;
        this.username = username;
        this.admin = admin;
    }
}

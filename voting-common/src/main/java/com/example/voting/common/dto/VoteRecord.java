package com.example.voting.common.dto;

public class VoteRecord {
    public String username;
    public int optionIndex;
    public String optionText;
    public String voteTime; // 字符串时间

    public VoteRecord() {}

    public VoteRecord(String username, int optionIndex, String optionText, String voteTime) {
        this.username = username;
        this.optionIndex = optionIndex;
        this.optionText = optionText;
        this.voteTime = voteTime;
    }
}

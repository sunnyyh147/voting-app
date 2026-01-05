// voting-common/src/main/java/com/example/voting/common/dto/VoteRequest.java
package com.example.voting.common.dto;

public class VoteRequest {
    public int pollId;
    public int optionIndex; // 0..n-1
}

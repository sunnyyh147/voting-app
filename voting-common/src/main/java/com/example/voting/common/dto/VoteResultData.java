// voting-common/src/main/java/com/example/voting/common/dto/VoteResultData.java
package com.example.voting.common.dto;

import java.util.List;

public class VoteResultData {
    public int pollId;
    public List<Long> counts;

    public VoteResultData() {}
    public VoteResultData(int pollId, List<Long> counts) {
        this.pollId = pollId;
        this.counts = counts;
    }
}

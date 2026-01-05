// voting-common/src/main/java/com/example/voting/common/dto/PollDetailData.java
package com.example.voting.common.dto;

import java.util.List;

public class PollDetailData {
    public int id;
    public String title;
    public boolean open;

    public List<String> options;
    public List<Long> counts;     // 与 options 对齐
    public Integer yourVoteIndex; // 你当前选择的选项(可空)

    public PollDetailData() {}
}

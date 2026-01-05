// voting-common/src/main/java/com/example/voting/common/dto/PollSummary.java
package com.example.voting.common.dto;

public class PollSummary {
    public int id;
    public String title;
    public boolean open;

    public PollSummary() {}
    public PollSummary(int id, String title, boolean open) {
        this.id = id;
        this.title = title;
        this.open = open;
    }

    @Override
    public String toString() {
        return "#" + id + " " + title + (open ? "  (进行中)" : "  (已关闭)");
    }
}

// voting-server/src/main/java/com/example/voting/server/http/PollsHandler.java
package com.example.voting.server.http;

import com.example.voting.common.dto.PollDetailData;
import com.example.voting.common.dto.PollSummary;
import com.example.voting.server.auth.AuthManager;
import com.example.voting.server.dao.PollDao;
import com.example.voting.server.dao.VoteDao;
import com.example.voting.server.util.HttpIO;
import com.example.voting.server.util.PathUtil;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class PollsHandler extends BaseHandler {
    private final PollDao pollDao;
    private final VoteDao voteDao;

    public PollsHandler(AuthManager auth, PollDao pollDao, VoteDao voteDao) {
        super(auth);
        this.pollDao = pollDao;
        this.voteDao = voteDao;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String user = requireUser(ex);
        if (user == null) return;

        try {
            String method = ex.getRequestMethod();
            String[] seg = PathUtil.segments(ex.getRequestURI().getPath());
            // /api/polls  或 /api/polls/{id}
            if (seg.length == 2 && "api".equals(seg[0]) && "polls".equals(seg[1])) {
                if (!"GET".equalsIgnoreCase(method)) {
                    HttpIO.fail(ex, 405, "Method Not Allowed");
                    return;
                }
                List<PollSummary> list = pollDao.listPolls();
                HttpIO.ok(ex, list);
                return;
            }

            if (seg.length == 3 && "api".equals(seg[0]) && "polls".equals(seg[1])) {
                if (!"GET".equalsIgnoreCase(method)) {
                    HttpIO.fail(ex, 405, "Method Not Allowed");
                    return;
                }
                Integer pollId = PathUtil.tryParseInt(seg[2]);
                if (pollId == null) {
                    HttpIO.fail(ex, 400, "pollId 非法");
                    return;
                }

                PollDao.PollRow poll = pollDao.getPoll(pollId);
                if (poll == null) {
                    HttpIO.fail(ex, 404, "投票不存在");
                    return;
                }

                List<String> options = pollDao.parseOptions(poll.optionsJson);
                List<Long> counts = voteDao.countByOption(pollId, options.size());
                Integer yourVote = voteDao.getUserVote(pollId, user);

                PollDetailData data = new PollDetailData();
                data.id = poll.id;
                data.title = poll.title;
                data.open = poll.open;
                data.options = options;
                data.counts = counts;
                data.yourVoteIndex = yourVote;

                HttpIO.ok(ex, data);
                return;
            }

            HttpIO.fail(ex, 404, "Not Found");
        } catch (SQLException e) {
            HttpIO.fail(ex, 500, "数据库错误: " + e.getMessage());
        } catch (Exception e) {
            HttpIO.fail(ex, 500, "服务器错误: " + e.getMessage());
        }
    }
}

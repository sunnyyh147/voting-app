// voting-server/src/main/java/com/example/voting/server/http/VoteHandler.java
package com.example.voting.server.http;

import com.example.voting.common.dto.VoteRequest;
import com.example.voting.common.dto.VoteResultData;
import com.example.voting.server.auth.AuthManager;
import com.example.voting.server.dao.PollDao;
import com.example.voting.server.dao.VoteDao;
import com.example.voting.server.util.HttpIO;
import com.example.voting.server.util.PathUtil;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class VoteHandler extends BaseHandler {
    private final PollDao pollDao;
    private final VoteDao voteDao;

    public VoteHandler(AuthManager auth, PollDao pollDao, VoteDao voteDao) {
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
            // /api/vote
            if (seg.length == 2 && "api".equals(seg[0]) && "vote".equals(seg[1])) {
                if ("POST".equalsIgnoreCase(method)) {
                    VoteRequest req = HttpIO.readJson(ex, VoteRequest.class);
                    if (req.pollId <= 0) {
                        HttpIO.fail(ex, 400, "pollId 非法");
                        return;
                    }

                    PollDao.PollRow poll = pollDao.getPoll(req.pollId);
                    if (poll == null) { HttpIO.fail(ex, 404, "投票不存在"); return; }
                    if (!poll.open) { HttpIO.fail(ex, 403, "投票已关闭"); return; }

                    List<String> options = pollDao.parseOptions(poll.optionsJson);
                    if (req.optionIndex < 0 || req.optionIndex >= options.size()) {
                        HttpIO.fail(ex, 400, "optionIndex 非法");
                        return;
                    }

                    Integer old = voteDao.getUserVote(req.pollId, user);
                    if (old == null) {
                        voteDao.insertVote(req.pollId, user, req.optionIndex); // C
                    } else {
                        voteDao.updateVote(req.pollId, user, req.optionIndex); // U
                    }

                    List<Long> counts = voteDao.countByOption(req.pollId, options.size());
                    HttpIO.ok(ex, new VoteResultData(req.pollId, counts));
                    return;
                }

                if ("DELETE".equalsIgnoreCase(method)) {
                    // DELETE /api/vote?pollId=1
                    String q = ex.getRequestURI().getQuery();
                    Integer pollId = null;
                    if (q != null) {
                        for (String part : q.split("&")) {
                            String[] kv = part.split("=");
                            if (kv.length == 2 && "pollId".equals(kv[0])) {
                                pollId = PathUtil.tryParseInt(kv[1]);
                            }
                        }
                    }
                    if (pollId == null || pollId <= 0) {
                        HttpIO.fail(ex, 400, "pollId 必填");
                        return;
                    }

                    PollDao.PollRow poll = pollDao.getPoll(pollId);
                    if (poll == null) { HttpIO.fail(ex, 404, "投票不存在"); return; }

                    voteDao.deleteVote(pollId, user); // D

                    List<String> options = pollDao.parseOptions(poll.optionsJson);
                    List<Long> counts = voteDao.countByOption(pollId, options.size());
                    HttpIO.ok(ex, new VoteResultData(pollId, counts));
                    return;
                }

                HttpIO.fail(ex, 405, "Method Not Allowed");
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

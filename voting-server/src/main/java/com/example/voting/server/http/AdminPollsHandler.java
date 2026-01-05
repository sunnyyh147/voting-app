package com.example.voting.server.http;

import com.example.voting.common.dto.CreatePollRequest;
import com.example.voting.common.dto.CreatePollResponseData;
import com.example.voting.common.dto.VoteRecord;
import com.example.voting.server.auth.AuthManager;
import com.example.voting.server.dao.PollDao;
import com.example.voting.server.dao.UserDao;
import com.example.voting.server.dao.VoteDao;
import com.example.voting.server.util.HttpIO;
import com.example.voting.server.util.PathUtil;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AdminPollsHandler extends BaseHandler {
    private final UserDao userDao;
    private final PollDao pollDao;
    private final VoteDao voteDao;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    public AdminPollsHandler(AuthManager auth, UserDao userDao, PollDao pollDao, VoteDao voteDao) {
        super(auth);
        this.userDao = userDao;
        this.pollDao = pollDao;
        this.voteDao = voteDao;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String user = requireUser(ex);
        if (user == null) return;

        try {
            if (!userDao.isAdmin(user)) {
                HttpIO.fail(ex, 403, "无权限（仅管理员）");
                return;
            }

            String[] seg = PathUtil.segments(ex.getRequestURI().getPath());
            // 期望：
            // POST   /api/admin/polls
            // DELETE /api/admin/polls/{id}
            // GET    /api/admin/polls/{id}/votes

            if (seg.length >= 3 && "api".equals(seg[0]) && "admin".equals(seg[1]) && "polls".equals(seg[2])) {

                // POST /api/admin/polls
                if (seg.length == 3 && "POST".equalsIgnoreCase(ex.getRequestMethod())) {
                    CreatePollRequest req = HttpIO.readJson(ex, CreatePollRequest.class);
                    if (req.title == null || req.title.isBlank()) {
                        HttpIO.fail(ex, 400, "title 不能为空");
                        return;
                    }
                    if (req.options == null || req.options.size() < 2) {
                        HttpIO.fail(ex, 400, "options 至少 2 个选项");
                        return;
                    }

                    List<String> cleaned = new ArrayList<>();
                    for (String s : req.options) {
                        if (s != null && !s.trim().isBlank()) cleaned.add(s.trim());
                    }
                    if (cleaned.size() < 2) {
                        HttpIO.fail(ex, 400, "options 至少 2 个非空选项");
                        return;
                    }

                    String optionsJson = pollDao.toOptionsJson(cleaned);
                    int id = pollDao.createPoll(req.title.trim(), optionsJson);
                    HttpIO.ok(ex, new CreatePollResponseData(id));
                    return;
                }

                // DELETE /api/admin/polls/{id}
                if (seg.length == 4 && "DELETE".equalsIgnoreCase(ex.getRequestMethod())) {
                    Integer pollId = PathUtil.tryParseInt(seg[3]);
                    if (pollId == null || pollId <= 0) {
                        HttpIO.fail(ex, 400, "pollId 非法");
                        return;
                    }
                    boolean ok = pollDao.deletePollWithVotes(pollId);
                    if (!ok) {
                        HttpIO.fail(ex, 404, "投票不存在");
                        return;
                    }
                    HttpIO.ok(ex, null);
                    return;
                }

                // GET /api/admin/polls/{id}/votes
                if (seg.length == 5 && "GET".equalsIgnoreCase(ex.getRequestMethod()) && "votes".equals(seg[4])) {
                    Integer pollId = PathUtil.tryParseInt(seg[3]);
                    if (pollId == null || pollId <= 0) {
                        HttpIO.fail(ex, 400, "pollId 非法");
                        return;
                    }

                    PollDao.PollRow poll = pollDao.getPoll(pollId);
                    if (poll == null) {
                        HttpIO.fail(ex, 404, "投票不存在");
                        return;
                    }
                    List<String> options = pollDao.parseOptions(poll.optionsJson);

                    List<VoteDao.VoteRow> rows = voteDao.listVotesByPoll(pollId);
                    List<VoteRecord> out = new ArrayList<>();
                    for (VoteDao.VoteRow r : rows) {
                        String optText = (r.optionIndex >= 0 && r.optionIndex < options.size())
                                ? options.get(r.optionIndex) : "(非法选项)";
                        String t = (r.voteTime == null) ? "" : FMT.format(r.voteTime.toInstant());
                        out.add(new VoteRecord(r.username, r.optionIndex, optText, t));
                    }
                    HttpIO.ok(ex, out);
                    return;
                }
            }

            HttpIO.fail(ex, 404, "Not Found");
        } catch (SQLException e) {
            HttpIO.fail(ex, 500, "数据库错误: " + e.getMessage());
        } catch (Exception e) {
            HttpIO.fail(ex, 500, "服务器错误: " + e.getMessage());
        }
    }
}

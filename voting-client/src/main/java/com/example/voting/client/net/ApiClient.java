// voting-client/src/main/java/com/example/voting/client/net/ApiClient.java
package com.example.voting.client.net;

import com.example.voting.client.model.Session;
import com.example.voting.client.util.ClientJson;
import com.example.voting.common.dto.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class ApiClient {
    private final Session session;
    private final HttpClient http;

    public ApiClient(Session session) {
        this.session = session;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public ApiResponse<LoginResponseData> login(String username, String password) throws Exception {
        LoginRequest reqObj = new LoginRequest();
        reqObj.username = username;
        reqObj.password = password;
        return post("/api/login", reqObj, LoginResponseData.class, false);
    }

    public ApiResponse<Void> register(String username, String password) throws Exception {
        RegisterRequest r = new RegisterRequest();
        r.username = username;
        r.password = password;
        return post("/api/register", r, Void.class, false);
    }

    public ApiResponse<List<PollSummary>> listPolls() throws Exception {
        return getList("/api/polls", PollSummary.class);
    }

    public ApiResponse<PollDetailData> pollDetail(int pollId) throws Exception {
        return get("/api/polls/" + pollId, PollDetailData.class);
    }

    public ApiResponse<VoteResultData> vote(int pollId, int optionIndex) throws Exception {
        VoteRequest vr = new VoteRequest();
        vr.pollId = pollId;
        vr.optionIndex = optionIndex;
        return post("/api/vote", vr, VoteResultData.class, true);
    }

    public ApiResponse<VoteResultData> deleteVote(int pollId) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(session.baseUrl + "/api/vote?pollId=" + pollId))
                .timeout(Duration.ofSeconds(8))
                .DELETE();
        if (session.isLoggedIn()) b.header("X-Auth-Token", session.token);

        HttpResponse<byte[]> resp = http.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
        return parse(resp, VoteResultData.class);
    }

    private <T> ApiResponse<T> get(String path, Class<T> dataClass) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(session.baseUrl + path))
                .timeout(Duration.ofSeconds(8))
                .GET();
        if (session.isLoggedIn()) b.header("X-Auth-Token", session.token);

        HttpResponse<byte[]> resp = http.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
        return parse(resp, dataClass);
    }

    private <E> ApiResponse<List<E>> getList(String path, Class<E> elementClass) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(session.baseUrl + path))
                .timeout(Duration.ofSeconds(8))
                .GET();
        if (session.isLoggedIn()) b.header("X-Auth-Token", session.token);

        HttpResponse<byte[]> resp = http.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
        var type = ClientJson.MAPPER.getTypeFactory()
                .constructParametricType(ApiResponse.class,
                        ClientJson.MAPPER.getTypeFactory().constructCollectionType(List.class, elementClass));

        return ClientJson.MAPPER.readValue(resp.body(), type);
    }

    private <T> ApiResponse<T> post(String path, Object bodyObj, Class<T> dataClass, boolean needAuth) throws Exception {
        byte[] body = ClientJson.MAPPER.writeValueAsBytes(bodyObj);

        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(session.baseUrl + path))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body));

        if (needAuth && session.isLoggedIn()) {
            b.header("X-Auth-Token", session.token);
        }

        HttpResponse<byte[]> resp = http.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
        return parse(resp, dataClass);
    }

    private <T> ApiResponse<T> parse(HttpResponse<byte[]> resp, Class<T> dataClass) throws Exception {
        var type = ClientJson.MAPPER.getTypeFactory()
                .constructParametricType(ApiResponse.class, dataClass);
        return ClientJson.MAPPER.readValue(resp.body(), type);
    }
}

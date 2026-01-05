// voting-server/src/main/java/com/example/voting/server/ServerMain.java
package com.example.voting.server;

import com.example.voting.server.auth.AuthManager;
import com.example.voting.server.dao.PollDao;
import com.example.voting.server.dao.UserDao;
import com.example.voting.server.dao.VoteDao;
import com.example.voting.server.db.DataSourceFactory;
import com.example.voting.server.db.DbInit;
import com.example.voting.server.http.LoginHandler;
import com.example.voting.server.http.PollsHandler;
import com.example.voting.server.http.RegisterHandler;
import com.example.voting.server.http.VoteHandler;
import com.sun.net.httpserver.HttpServer;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
    private static final Logger log = LoggerFactory.getLogger(ServerMain.class);

    public static void main(String[] args) throws Exception {
        Properties props = loadProps();

        int port = Integer.parseInt(props.getProperty("server.port", "8080"));

        HikariDataSource ds = DataSourceFactory.create(props);
        DbInit.init(ds);

        AuthManager auth = new AuthManager();
        UserDao userDao = new UserDao(ds);
        PollDao pollDao = new PollDao(ds);
        VoteDao voteDao = new VoteDao(ds);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // 线程池：并发处理请求（多线程评分点）
        ExecutorService pool = Executors.newFixedThreadPool(16);
        server.setExecutor(pool);

        server.createContext("/api/login", new LoginHandler(auth, userDao));
        server.createContext("/api/register", new RegisterHandler(auth, userDao));
        server.createContext("/api/polls", new PollsHandler(auth, pollDao, voteDao)); // 同时处理 /api/polls/{id}
        server.createContext("/api/vote", new VoteHandler(auth, pollDao, voteDao));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            server.stop(0);
            pool.shutdownNow();
            ds.close();
        }));

        server.start();
        log.info("Server started at http://127.0.0.1:{}/", port);
        log.info("Default user: test / 123456");
    }

    private static Properties loadProps() throws Exception {
        Properties props = new Properties();
        try (InputStream is = ServerMain.class.getClassLoader().getResourceAsStream("server.properties")) {
            if (is == null) throw new IllegalStateException("server.properties not found in resources");
            props.load(is);
        }
        return props;
    }
}

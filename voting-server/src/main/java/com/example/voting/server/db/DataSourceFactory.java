// voting-server/src/main/java/com/example/voting/server/db/DataSourceFactory.java
package com.example.voting.server.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.Properties;

public final class DataSourceFactory {
    private DataSourceFactory() {}

    public static HikariDataSource create(Properties props) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(props.getProperty("db.url"));
        cfg.setUsername(props.getProperty("db.user"));
        cfg.setPassword(props.getProperty("db.password"));

        cfg.setMaximumPoolSize(10);
        cfg.setMinimumIdle(2);
        cfg.setConnectionTimeout(8000);
        cfg.setIdleTimeout(60000);

        // MySQL 推荐
        cfg.addDataSourceProperty("cachePrepStmts", "true");
        cfg.addDataSourceProperty("prepStmtCacheSize", "250");
        cfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        cfg.addDataSourceProperty("useServerPrepStmts", "true");

        return new HikariDataSource(cfg);
    }
}

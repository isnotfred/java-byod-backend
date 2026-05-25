package com.pup.byod.javabyodbackend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Value("${PGHOST}")
    private String pgHost;

    @Value("${PGPORT}")
    private String pgPort;

    @Value("${PGDATABASE}")
    private String pgDatabase;

    @Value("${PGUSER}")
    private String pgUser;

    @Value("${PGPASSWORD}")
    private String pgPassword;

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl("jdbc:postgresql://" + pgHost + ":" + pgPort + "/" + pgDatabase);
        config.setUsername(pgUser);
        config.setPassword(pgPassword);
        config.setDriverClassName("org.postgresql.Driver");

        // Pool settings
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);

        // Railway PostgreSQL requires SSL
        config.addDataSourceProperty("sslmode", "require");

        config.setPoolName("byod-hikari-pool");

        return new HikariDataSource(config);
    }
}
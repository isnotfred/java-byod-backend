package com.pup.byod.javabyodbackend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Value("${PGHOST:localhost}")
    private String pgHost;

    @Value("${PGPORT:5432}")
    private String pgPort;

    @Value("${PGDATABASE:byod}")
    private String pgDatabase;

    @Value("${PGUSER:postgres}")
    private String pgUser;

    @Value("${PGPASSWORD:postgres}")
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
        config.setInitializationFailTimeout(-1);

        // Railway PostgreSQL requires SSL
        config.addDataSourceProperty("sslmode", "require");

        config.setPoolName("byod-hikari-pool");

        return new HikariDataSource(config);
    }
}
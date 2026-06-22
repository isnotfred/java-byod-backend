package com.pup.byod.javabyodbackend;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@SpringBootTest(properties = {
    "PGHOST=localhost",
    "PGPORT=5432",
    "PGDATABASE=testdb",
    "PGUSER=postgres",
    "PGPASSWORD=postgres",
    "spring.main.allow-bean-definition-overriding=true"
})
class JavaByodBackendApplicationTests {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public DataSource dataSource() {
            return Mockito.mock(DataSource.class);
        }
    }

    @Test
    void contextLoads() {
    }
}

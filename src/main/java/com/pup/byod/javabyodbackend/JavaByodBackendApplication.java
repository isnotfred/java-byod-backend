package com.pup.byod.javabyodbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
public class JavaByodBackendApplication {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Manila"));
    }

    public static void main(String[] args) {
        SpringApplication.run(JavaByodBackendApplication.class, args);
    }

}

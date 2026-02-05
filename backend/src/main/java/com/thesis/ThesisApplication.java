package com.thesis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.thesis.mapper")
public class ThesisApplication {
    public static void main(String[] args) {
        SpringApplication.run(ThesisApplication.class, args);
    }
}

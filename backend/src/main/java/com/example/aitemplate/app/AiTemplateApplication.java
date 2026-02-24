package com.example.aitemplate.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.aitemplate")
@MapperScan("com.example.aitemplate.infra.db.mapper")
public class AiTemplateApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiTemplateApplication.class, args);
    }
}

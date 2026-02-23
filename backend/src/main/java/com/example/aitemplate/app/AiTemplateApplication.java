package com.example.aitemplate.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.aitemplate")
public class AiTemplateApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiTemplateApplication.class, args);
    }
}

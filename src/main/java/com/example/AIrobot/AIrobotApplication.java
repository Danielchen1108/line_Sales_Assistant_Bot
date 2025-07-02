package com.example.AIrobot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class AIrobotApplication {
    public static void main(String[] args) {
        // 手動載入 .env
        Dotenv dotenv = Dotenv.configure()
                .directory(".") // 指定 .env 路徑（與 pom.xml 同層）
                .ignoreIfMissing()
                .load();

        // 測試印出變數
        System.out.println("✔️ ENV OPENAI_API_KEY = " + dotenv.get("OPENAI_API_KEY"));

        SpringApplication.run(AIrobotApplication.class, args);
    }
}

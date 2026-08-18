package com.bank.messaging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class FinancialMessagingPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinancialMessagingPlatformApplication.class, args);
    }
}

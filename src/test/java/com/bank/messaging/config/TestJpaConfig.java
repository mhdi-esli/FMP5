package com.bank.messaging.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Test configuration for JPA repositories.
 * This configuration is used for integration tests.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.bank.messaging.repository")
@EnableTransactionManagement
public class TestJpaConfig {
}

package com.bank.messaging.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for API documentation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI financialMessagingOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Financial Messaging Platform API")
                .description("MT200 Financial Institution Transfer Message Service")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Messaging Team")
                    .email("messaging@bank.com")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Development server")
            ));
    }
}

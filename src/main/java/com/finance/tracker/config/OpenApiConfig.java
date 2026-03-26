package com.finance.tracker.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI financeTrackerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Finance Tracker API")
                        .version("v1")
                        .description("""
                                REST API for managing users, accounts, budgets, categories and transactions.
                                The documentation also exposes validation rules and the unified error format
                                introduced in laboratory work 4.
                                """)
                        .contact(new Contact()
                                .name("Finance Tracker Lab"))
                        .license(new License()
                                .name("For educational use")));
    }
}

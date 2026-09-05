package com.urlshortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 (Swagger) specification configuration.
 * Interactive documentation available at: /swagger-ui/index.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI urlShortenerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("URL Shortener REST API")
                        .description("High-performance URL shortening and analytics platform with Redis caching, rate limiting, and QR code generation.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Muhammet Alğan")
                                .url("https://github.com/muhammet-algan")
                                .email("muhammetalgann@gmail.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}

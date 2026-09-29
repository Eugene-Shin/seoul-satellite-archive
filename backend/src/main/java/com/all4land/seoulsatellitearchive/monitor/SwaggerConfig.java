package com.all4land.seoulsatellitearchive.monitor;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI seoulsatellitearchiveOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Seoul Satellite Archive BE API")
                        .description("Seoul Satellite Archive backend API documentation")
                        .version("v1")
                        .contact(new Contact().name("ALL4Land"))
                        .license(new License().name("Internal Use")));
    }
}

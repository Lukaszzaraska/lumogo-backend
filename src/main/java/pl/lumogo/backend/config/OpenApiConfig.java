package pl.lumogo.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .components(new Components().addSecuritySchemes("bearerAuth",
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
            .info(new Info()
                .title("LumoGo Backend API")
                .version("0.0.1")
                .description("Backend aplikacji mobilnej dla biegaczy i osób aktywnych. Zawiera endpointy do autentykacji, zarządzania zainteresowaniami i pobierania warunków pogodowych.")
                .contact(new Contact()
                    .name("LumoGo Team")
                    .url("https://lumogo.pl"))
                .license(new License()
                    .name("MIT License")));
    }
}

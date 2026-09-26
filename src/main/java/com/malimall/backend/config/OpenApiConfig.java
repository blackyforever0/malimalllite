package com.malimall.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentation vivante (springdoc-openapi) sur /swagger-ui.html — déclare le
 * schéma "Bearer JWT" pour que le bouton "Authorize" de Swagger fonctionne
 * directement avec le token renvoyé par /api/auth/connexion.
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME_JWT = "bearerAuth";

    @Bean
    public OpenAPI maliMallOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("MaliMall — API")
                        .description("Parcours Market : catalogue, panier, commande, livraison à deux codes, portefeuille.")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_JWT))
                .components(new Components().addSecuritySchemes(SCHEME_JWT,
                        new SecurityScheme()
                                .name(SCHEME_JWT)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}

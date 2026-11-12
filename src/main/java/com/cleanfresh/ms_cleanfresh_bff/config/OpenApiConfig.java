package com.cleanfresh.ms_cleanfresh_bff.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI del BFF: {@code /swagger-ui.html} y {@code /v3/api-docs}. La documentación es pública (solo describe el contrato);
 * para probar un endpoint desde Swagger hay que pulsar "Authorize" y pegar un access token de Cognito, que es lo que el BFF valida.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA = "cognito-access-token";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ms-cleanfresh-bff")
                        .version("0.0.1")
                        .description("BFF de Clean&Fresh Manager: la única entrada del sistema. Valida el JWT de AWS Cognito (emisor, firma, vigencia, "
                                + "token_use, client_id y scope), aplica la autorización por rol (Admin, Operador, Cliente) y reenvía a los microservicios."))
                .components(new Components().addSecuritySchemes(ESQUEMA,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                                .description("Access token de Cognito (no el idToken)")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA));
    }
}

package br.com.raizesdonordeste_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/** Documentação OpenAPI com o botão "Authorize" para informar o token JWT no Swagger UI. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI documentacaoApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Raízes do Nordeste")
                        .version("v1")
                        .description("API Back-end da rede de lanchonetes Raízes do Nordeste — "
                                + "Projeto Multidisciplinar UNINTER (trilha Back-end)."))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}

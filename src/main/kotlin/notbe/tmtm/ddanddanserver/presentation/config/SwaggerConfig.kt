package notbe.tmtm.ddanddanserver.presentation.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.servers.Server
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@OpenAPIDefinition(
    servers = [
        Server(
            url = "https://ddan-ddan.com/",
            description = "Production API server URL",
        ),
        Server(
            url = "http://localhost:8080/",
            description = "Local API server URL",
        ),
    ],
)
@Configuration
class SwaggerConfig {
    @Bean
    fun openApi(): OpenAPI {
        val securityRequirement = SecurityRequirement().addList("JWT")
        val component =
            Components().addSecuritySchemes(
                "JWT",
                SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"),
            )
        return OpenAPI().addSecurityItem(securityRequirement).components(component)
    }
}

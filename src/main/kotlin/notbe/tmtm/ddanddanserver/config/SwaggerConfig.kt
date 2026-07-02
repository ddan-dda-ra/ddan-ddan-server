package notbe.tmtm.ddanddanserver.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.servers.Server
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import notbe.tmtm.ddanddanserver.presentation.filter.PetCatalogRevisionFilter
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@OpenAPIDefinition(
    servers = [
        Server(
            url = "https://dev-ddan-ddan-api.ddmz.org/",
            description = "Development API server URL",
        ),
        Server(
            url = "https://ddan-ddan-api.ddmz.org/",
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

    @Bean
    fun petCatalogRevisionOpenApiCustomizer(): OpenApiCustomizer =
        OpenApiCustomizer { openApi ->
            openApi.paths
                ?.filterKeys { it != LOGIN_PATH }
                ?.values
                ?.flatMap { it.readOperations() }
                ?.forEach { operation ->
                    operation.addParametersItem(
                        Parameter()
                            .name(PetCatalogRevisionFilter.REQUEST_REVISION_HEADER)
                            .`in`("header")
                            .required(true)
                            .schema(StringSchema().format("date-time"))
                            .description("클라이언트가 저장한 펫 카탈로그 revision"),
                    )
                    operation.responses?.values?.forEach { response ->
                        response.addHeaderObject(
                            PetCatalogRevisionFilter.DOWNLOAD_REQUIRED_HEADER,
                            Header()
                                .schema(BooleanSchema())
                                .description("펫 카탈로그와 에셋 갱신 필요 여부"),
                        )
                    }
                }
        }

    companion object {
        private const val LOGIN_PATH = "/v1/auth/login"
    }
}

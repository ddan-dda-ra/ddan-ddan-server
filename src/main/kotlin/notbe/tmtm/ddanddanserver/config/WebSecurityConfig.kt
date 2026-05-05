package notbe.tmtm.ddanddanserver.config

import notbe.tmtm.ddanddanserver.common.JWTTokenProvider
import notbe.tmtm.ddanddanserver.domain.exception.PermissionDeniedException
import notbe.tmtm.ddanddanserver.domain.exception.UnauthorizedException
import notbe.tmtm.ddanddanserver.presentation.filter.AppVersionFilter
import notbe.tmtm.ddanddanserver.presentation.filter.JWTAuthFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.servlet.HandlerExceptionResolver

@Configuration
@EnableWebSecurity
class WebSecurityConfig(
    private val environment: Environment,
) {
    @Bean
    @Order(0)
    fun adminFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .securityMatcher("/v1/admin/**")
            .csrf { it.disable() }
            .cors { it.configurationSource(adminCorsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            // ⚠️ Phase 1: 인증 없이 노출. Phase 2에서 admin JWT 보호 추가 예정.
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .build()

    private fun adminCorsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins =
                listOf(
                    "https://admin-ddan-ddan.ddmz.org",
                    "http://localhost:3000",
                    "http://localhost:3001",
                )
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
            maxAge = 3600L
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }

    @Bean
    @Order(1)
    fun apiFilterChain(
        http: HttpSecurity,
        jwtTokenProvider: JWTTokenProvider,
        handlerExceptionResolver: HandlerExceptionResolver,
    ): SecurityFilterChain =
        http
            .securityMatcher("/v1/**")
            .csrf { it.disable() }
            .cors { if (isDevProfile()) it.configure(http) }
            .authorizeHttpRequests { it.anyRequest().authenticated() }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }.addFilterBefore(
                AppVersionFilter(handlerExceptionResolver),
                UsernamePasswordAuthenticationFilter::class.java,
            ).addFilterAfter(
                JWTAuthFilter(jwtTokenProvider, handlerExceptionResolver),
                AppVersionFilter::class.java,
            ).exceptionHandling {
                it
                    .accessDeniedHandler { request, response, exception ->
                        handlerExceptionResolver.resolveException(request, response, null, PermissionDeniedException())
                    }.authenticationEntryPoint { request, response, authException ->
                        handlerExceptionResolver.resolveException(request, response, null, UnauthorizedException())
                    }
            }.build()

    @Bean
    @Order(2)
    fun loginFilterChain(httpSecurity: HttpSecurity): SecurityFilterChain =
        httpSecurity
            .securityMatcher("/v1/auth/**", "/swagger-ui/index.html")
            .csrf { it.disable() }
            .cors { if (isDevProfile()) it.configure(httpSecurity) }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }.authorizeHttpRequests {
                it.anyRequest().permitAll()
            }.build()

    private fun isDevProfile(): Boolean = environment.activeProfiles.contains("dev")
}

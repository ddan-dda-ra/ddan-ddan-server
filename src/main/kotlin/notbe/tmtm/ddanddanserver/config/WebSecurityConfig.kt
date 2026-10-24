package notbe.tmtm.ddanddanserver.config

import notbe.tmtm.ddanddanserver.application.service.PetCatalogService
import notbe.tmtm.ddanddanserver.common.JWTTokenProvider
import notbe.tmtm.ddanddanserver.domain.exception.PermissionDeniedException
import notbe.tmtm.ddanddanserver.domain.exception.UnauthorizedException
import notbe.tmtm.ddanddanserver.presentation.filter.AppVersionFilter
import notbe.tmtm.ddanddanserver.presentation.filter.JWTAuthFilter
import notbe.tmtm.ddanddanserver.presentation.filter.PetCatalogRevisionFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.servlet.HandlerExceptionResolver

@Configuration
@EnableWebSecurity
class WebSecurityConfig(
    private val environment: Environment,
    private val petCatalogService: PetCatalogService,
    private val appVersionProperties: AppVersionProperties,
) {
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
                AppVersionFilter(
                    handlerExceptionResolver = handlerExceptionResolver,
                    jwtTokenProvider = jwtTokenProvider,
                    androidMinimumVersion = appVersionProperties.android,
                    iosMinimumVersion = appVersionProperties.ios,
                ),
                UsernamePasswordAuthenticationFilter::class.java,
            ).addFilterBefore(
                PetCatalogRevisionFilter(petCatalogService),
                AppVersionFilter::class.java,
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
    @Order(0)
    fun loginFilterChain(httpSecurity: HttpSecurity): SecurityFilterChain =
        httpSecurity
            .securityMatcher("/v1/auth/**", "/swagger-ui/index.html")
            .csrf { it.disable() }
            .cors { if (isDevProfile()) it.configure(httpSecurity) }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }.addFilterBefore(
                PetCatalogRevisionFilter(petCatalogService),
                UsernamePasswordAuthenticationFilter::class.java,
            ).authorizeHttpRequests {
                it.anyRequest().permitAll()
            }.build()

    // 위 두 체인에 매칭되지 않는 모든 요청(/actuator/**, /v3/api-docs 등)을 처리하는 최후 체인.
    // securityMatcher가 없으므로 반드시 가장 큰 Order여야 한다. (작으면 /v1/** 요청까지 가로챈다)
    // 헬스체크, API 문서(springdoc 활성 프로필만 실제 노출), /error만 허용하고 나머지는 403으로 거부한다.
    @Bean
    @Order(2)
    fun fallbackFilterChain(httpSecurity: HttpSecurity): SecurityFilterChain =
        httpSecurity
            .csrf { it.disable() }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }.authorizeHttpRequests {
                it
                    .requestMatchers(*PUBLIC_PATHS)
                    .permitAll()
                    .anyRequest()
                    .denyAll()
            }.exceptionHandling {
                it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.FORBIDDEN))
            }.build()

    private fun isDevProfile(): Boolean = environment.activeProfiles.contains("dev")

    companion object {
        private val PUBLIC_PATHS =
            arrayOf(
                "/actuator/health",
                "/actuator/health/**",
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/api-docs",
                "/v3/api-docs/**",
                "/error",
            )
    }
}

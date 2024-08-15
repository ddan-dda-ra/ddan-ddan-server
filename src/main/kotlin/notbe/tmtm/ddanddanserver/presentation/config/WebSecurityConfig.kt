package notbe.tmtm.ddanddanserver.presentation.config

import notbe.tmtm.ddanddanserver.domain.exception.PermissionDeniedException
import notbe.tmtm.ddanddanserver.domain.exception.UnauthorizedException
import notbe.tmtm.ddanddanserver.infrastructure.token.JWTTokenProvider
import notbe.tmtm.ddanddanserver.presentation.filter.JWTAuthFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.servlet.HandlerExceptionResolver

@Configuration
@EnableWebSecurity
class WebSecurityConfig {
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
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests { it.anyRequest().authenticated() }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }.addFilterBefore(
                JWTAuthFilter(jwtTokenProvider, handlerExceptionResolver),
                UsernamePasswordAuthenticationFilter::class.java,
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
            .cors(Customizer.withDefaults())
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }.authorizeHttpRequests {
                it.anyRequest().permitAll()
            }.build()
}

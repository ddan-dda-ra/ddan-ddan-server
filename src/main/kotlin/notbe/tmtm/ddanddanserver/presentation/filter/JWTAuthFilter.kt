package notbe.tmtm.ddanddanserver.presentation.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import notbe.tmtm.ddanddanserver.domain.exception.AuthenticationInvalidTokenException
import notbe.tmtm.ddanddanserver.domain.exception.AuthenticationTokenNotExistException
import notbe.tmtm.ddanddanserver.infrastructure.token.JWTTokenProvider
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerExceptionResolver

class JWTAuthFilter(
    private val jwtTokenProvider: JWTTokenProvider,
    private val handlerExceptionResolver: HandlerExceptionResolver,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            val authenticationHeader = request.getHeader("Authorization") ?: throw AuthenticationTokenNotExistException()
            val accessToken =
                if (authenticationHeader.startsWith("Bearer ")) {
                    authenticationHeader.substring(7)
                } else {
                    throw AuthenticationInvalidTokenException()
                }

            val authentication = jwtTokenProvider.parseToken(accessToken)
            SecurityContextHolder.getContext().authentication = authentication
            return filterChain.doFilter(request, response)
        } catch (e: Exception) {
            handlerExceptionResolver.resolveException(request, response, null, e)
        }
    }
}

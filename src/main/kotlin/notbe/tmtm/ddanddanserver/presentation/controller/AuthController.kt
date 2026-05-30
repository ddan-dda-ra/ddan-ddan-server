package notbe.tmtm.ddanddanserver.presentation.controller

import io.swagger.v3.oas.annotations.tags.Tag
import notbe.tmtm.ddanddanserver.application.service.AuthService
import notbe.tmtm.ddanddanserver.presentation.dto.request.LoginRequest
import notbe.tmtm.ddanddanserver.presentation.dto.request.RefreshTokenRequest
import notbe.tmtm.ddanddanserver.presentation.dto.response.LoginResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/auth")
@Tag(name = "로그인 & 인증")
class AuthController(
    val authService: AuthService,
) {
    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
        @RequestHeader(name = "X-Mock-OAuth", required = false, defaultValue = "false") useMock: Boolean,
    ): LoginResponse {
        val result = authService.login(request.token, request.tokenType, request.deviceToken, useMock)

        return LoginResponse.fromDomain(
            accessToken = result.accessToken,
            refreshToken = result.refreshToken,
            user = result.user,
        )
    }

    @PostMapping("/reissue")
    fun refresh(
        @RequestBody request: RefreshTokenRequest,
    ): LoginResponse {
        val result = authService.reissueToken(request.refreshToken)

        return LoginResponse.fromDomain(
            accessToken = result.accessToken,
            refreshToken = result.refreshToken,
            user = result.user,
        )
    }
}

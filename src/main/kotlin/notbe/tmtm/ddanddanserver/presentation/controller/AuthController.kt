package notbe.tmtm.ddanddanserver.presentation.controller

import io.swagger.v3.oas.annotations.tags.Tag
import notbe.tmtm.ddanddanserver.domain.usecase.auth.LoginOAuth
import notbe.tmtm.ddanddanserver.domain.usecase.auth.ReissueToken
import notbe.tmtm.ddanddanserver.presentation.dto.request.LoginRequest
import notbe.tmtm.ddanddanserver.presentation.dto.request.RefreshTokenRequest
import notbe.tmtm.ddanddanserver.presentation.dto.response.LoginResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/auth")
@Tag(name = "로그인 & 인증")
class AuthController(
    val loginOAuth: LoginOAuth,
    val reissueToken: ReissueToken,
) {
    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
    ): LoginResponse {
        val result =
            loginOAuth.execute(
                LoginOAuth.LoginUserInput(
                    accessToken = request.token,
                    tokenType = request.tokenType,
                ),
            )

        return LoginResponse.fromDomain(
            accessToken = result.accessToken,
            refreshToken = result.refreshToken,
            user = result.user,
            isOnboardingComplete = result.isOnboardingComplete,
        )
    }

    @PostMapping("/reissue")
    fun refresh(
        @RequestBody request: RefreshTokenRequest,
    ): LoginResponse {
        val result =
            reissueToken.execute(
                ReissueToken.ReissueTokenInput(
                    refreshToken = request.refreshToken,
                ),
            )

        return LoginResponse.fromDomain(
            accessToken = result.accessToken,
            refreshToken = result.refreshToken,
            user = result.user,
            isOnboardingComplete = true,
        )
    }
}

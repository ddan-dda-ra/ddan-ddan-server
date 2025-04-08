package notbe.tmtm.ddanddanserver.domain.usecase.auth

import notbe.tmtm.ddanddanserver.common.util.logger
import notbe.tmtm.ddanddanserver.domain.exception.OAuthenticationInvalidTokenException
import notbe.tmtm.ddanddanserver.domain.gateway.AuthGateway
import notbe.tmtm.ddanddanserver.domain.gateway.TokenGateway
import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.model.auth.Auth
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import notbe.tmtm.ddanddanserver.infrastructure.client.OAuthClientFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LoginOAuth(
    private val oAuthClientFactory: OAuthClientFactory,
    private val authGateway: AuthGateway,
    private val userGateway: UserGateway,
    private val tokenGateway: TokenGateway,
) : UseCase<LoginOAuth.LoginUserInput, LoginOAuth.LoginUserOutput> {
    val logger = logger()

    data class LoginUserInput(
        val accessToken: String,
        val tokenType: OAuthType,
        val deviceToken: String?,
    )

    data class LoginUserOutput(
        val accessToken: String,
        val refreshToken: String,
        val user: User,
        val isOnboardingComplete: Boolean,
    )

    @Transactional
    override fun execute(input: LoginUserInput): LoginUserOutput {
        // 로그인 방식에 따라 오어스 정보를 가져온다.
        val oAuth = getOAuthInfo(input.accessToken, input.tokenType)

        // 해당 오어스 id 값에 맞는 auth 정보 찾아오기
        val auth = authGateway.findByOAuthIdAndType(oAuth.id, input.tokenType)

        // 가입되지 않은 유저의 경우 등록 후 토큰 발급
        if (auth == null) {
            val newUser = userGateway.save(User.register(name = oAuth.nickName, deviceToken = input.deviceToken))
            authGateway.save(
                Auth.create(oAuthId = oAuth.id, type = input.tokenType, userId = newUser.id),
            )
            return LoginUserOutput(
                accessToken = tokenGateway.createAccessToken(newUser),
                refreshToken = tokenGateway.createRefreshToken(newUser),
                user = newUser,
                isOnboardingComplete = newUser.hasMainPet(),
            )
        }
        // 가입된 유저의 경우 토큰 발급 & 디바이스 토큰 갱신
        val user = userGateway.getById(auth.userId)
        user.deviceToken = input.deviceToken ?: user.deviceToken
        userGateway.update(user)

        return LoginUserOutput(
            accessToken = tokenGateway.createAccessToken(user),
            refreshToken = tokenGateway.createRefreshToken(user),
            user = user,
            isOnboardingComplete = user.hasMainPet(),
        )
    }

    private fun getOAuthInfo(
        accessToken: String,
        oAuthType: OAuthType,
    ): OAuth {
        oAuthClientFactory.getClient(oAuthType).let { oAuthClient ->
            return try {
                oAuthClient.getOAuth(accessToken)
            } catch (e: Exception) {
                logger.error("loginError accessToken: $accessToken, error: ${e.message}")
                throw OAuthenticationInvalidTokenException(oAuthType)
            }
        }
    }
}

package notbe.tmtm.ddanddanserver.domain.usecase.auth

import notbe.tmtm.ddanddanserver.domain.exception.OAuthenticationInvalidTokenException
import notbe.tmtm.ddanddanserver.domain.gateway.AuthGateway
import notbe.tmtm.ddanddanserver.domain.gateway.OAuthGateway
import notbe.tmtm.ddanddanserver.domain.gateway.TokenGateway
import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.model.User
import notbe.tmtm.ddanddanserver.domain.model.auth.Auth
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthInfo
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import org.springframework.stereotype.Component

@Component
class LoginOAuth(
    private val oAuthGateway: OAuthGateway,
    private val authGateway: AuthGateway,
    private val userGateway: UserGateway,
    private val tokenGateway: TokenGateway,
) : UseCase<LoginOAuth.LoginUserInput, LoginOAuth.LoginUserOutput> {
    data class LoginUserInput(
        val accessToken: String,
        val tokenType: OAuthType,
    )

    data class LoginUserOutput(
        val accessToken: String,
        val refreshToken: String,
        val user: User,
        val isOnboardingComplete: Boolean,
    )

    override fun execute(input: LoginUserInput): LoginUserOutput {
        // 로그인 방식에 따라 오어스 정보를 가져온다.
        val oAuthInfo = getOAuthInfo(input.accessToken, input.tokenType)

        // 해당 오어스 id 값에 맞는 auth 정보 찾아오기
        val auth = authGateway.findByOAuthIdAndType(oAuthInfo.id, input.tokenType)

        // 가입되지 않은 유저의 경우 등록 후 토큰 발급
        if (auth == null) {
            val newUser = userGateway.save(User.register(name = oAuthInfo.nickName))
            authGateway.save(
                Auth.create(oAuthId = oAuthInfo.id, type = input.tokenType, userId = newUser.id),
            )
            return LoginUserOutput(
                accessToken = tokenGateway.createAccessToken(newUser),
                refreshToken = tokenGateway.createRefreshToken(newUser),
                user = newUser,
                isOnboardingComplete = newUser.hasMainPet(),
            )
        }
        // 가입된 유저의 경우 토큰 발급
        val user = userGateway.getById(auth.userId)

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
    ): OAuthInfo {
        when (oAuthType) {
            OAuthType.KAKAO -> {
                return try {
                    oAuthGateway.getOAuthUserInfo(accessToken)
                } catch (e: Exception) {
                    throw OAuthenticationInvalidTokenException(oAuthType)
                }
            }
            // TODO: apple login 정보 추가 필요
            else -> throw Exception("Not Supported Token Type")
        }
    }
}

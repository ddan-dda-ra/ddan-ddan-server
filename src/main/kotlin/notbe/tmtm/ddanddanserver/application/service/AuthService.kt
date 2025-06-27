package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.application.processor.OAuth
import notbe.tmtm.ddanddanserver.application.processor.OAuthProcessorFactory
import notbe.tmtm.ddanddanserver.common.JWTTokenProvider
import notbe.tmtm.ddanddanserver.domain.model.auth.Auth
import notbe.tmtm.ddanddanserver.domain.model.auth.AuthResult
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.AuthRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JWTTokenProvider,
    private val oauthProcessorFactory: OAuthProcessorFactory,
) {
    @Transactional
    fun login(
        oAuthAccessToken: String,
        oAuthType: OAuthType,
        deviceToken: String?,
    ): AuthResult {
        val oAuth = oauthProcessorFactory.getClient(oAuthType).getOAuth(oAuthAccessToken)
        authRepository.findByOAuthIdAndType(oAuth.id, oAuthType)?.let { auth ->
            return loginExistUser(auth, deviceToken)
        }

        return loginNewUser(oAuth, deviceToken, oAuthType)
    }

    @Transactional
    fun reissueToken(refreshToken: String): AuthResult {
        jwtTokenProvider.validateRefreshToken(refreshToken)
        val userId = jwtTokenProvider.getUserIdFromRefreshToken(refreshToken)

        val user = userRepository.findByIdOrNull(userId) ?: throw IllegalArgumentException("리프레시 토큰에 해당하는 유저가 없습니다")
        return AuthResult(
            accessToken = jwtTokenProvider.createAccessToken(user),
            refreshToken = jwtTokenProvider.createRefreshToken(user),
            user = user,
        )
    }

    private fun loginNewUser(
        oAuth: OAuth,
        deviceToken: String?,
        oAuthType: OAuthType,
    ): AuthResult {
        // 가입되지 않은 유저 CASE
        val newUser =
            userRepository.save(
                User.register(name = oAuth.nickName, deviceToken = deviceToken),
            )
        authRepository.save(
            Auth.create(oAuthId = oAuth.id, type = oAuthType, userId = newUser.id),
        )
        return AuthResult(
            accessToken = jwtTokenProvider.createAccessToken(newUser),
            refreshToken = jwtTokenProvider.createRefreshToken(newUser),
            user = newUser,
        )
    }

    private fun loginExistUser(
        auth: Auth,
        deviceToken: String?,
    ): AuthResult {
        // 가입된 유저 CASE
        val user =
            userRepository.findByIdOrNull(auth.userId)
                ?: throw RuntimeException("auth와 user의 정합성 불일치")

        // 디바이스 토큰 갱신
        deviceToken?.let { updateDeviceToken(it, user) }

        return AuthResult(
            accessToken = jwtTokenProvider.createAccessToken(user),
            refreshToken = jwtTokenProvider.createRefreshToken(user),
            user = user,
        )
    }

    private fun updateDeviceToken(
        deviceToken: String,
        user: User,
    ) {
        if (user.deviceToken != deviceToken) {
            user.updateDeviceToken(deviceToken)
            userRepository.save(user)
        }
    }
}

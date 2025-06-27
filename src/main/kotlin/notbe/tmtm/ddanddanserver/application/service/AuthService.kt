package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.application.processor.OAuthInfo
import notbe.tmtm.ddanddanserver.application.processor.OAuthProcessorFactory
import notbe.tmtm.ddanddanserver.common.JWTTokenProvider
import notbe.tmtm.ddanddanserver.common.util.logger
import notbe.tmtm.ddanddanserver.domain.exception.OAuthenticationInvalidTokenException
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
    val logger = logger()

    @Transactional
    fun login(
        oAuthAccessToken: String,
        oAuthType: OAuthType,
        deviceToken: String?,
    ): AuthResult {
        // 로그인 방식에 따라 오어스 정보를 가져온다.
        val oAuth: OAuthInfo = getOAuthInfo(oAuthAccessToken, oAuthType)

        // 해당 오어스 id 값에 맞는 auth 정보 찾아오기
        val auth = authRepository.findByOAuthIdAndType(oAuth.id, oAuthType)

        // 가입되지 않은 유저의 경우 등록 후 토큰 발급
        if (auth == null) {
            val newUser = userRepository.save(User.register(name = oAuth.nickName, deviceToken = deviceToken))
            authRepository.save(
                Auth.create(oAuthId = oAuth.id, type = oAuthType, userId = newUser.id),
            )
            return AuthResult(
                accessToken = jwtTokenProvider.createAccessToken(newUser),
                refreshToken = jwtTokenProvider.createRefreshToken(newUser),
                user = newUser,
            )
        }
        // 가입된 유저의 경우 토큰 발급 & 디바이스 토큰 갱신
        val user = userRepository.findByIdOrNull(auth.userId)
            ?: throw RuntimeException("auth와 user의 정합성 불일치")

        // 디바이스 토큰 갱신
        deviceToken?.let { updateDeviceToken(it, user) }

        return AuthResult(
            accessToken = jwtTokenProvider.createAccessToken(user),
            refreshToken = jwtTokenProvider.createRefreshToken(user),
            user = user,
        )
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

    private fun getOAuthInfo(
        accessToken: String,
        oAuthType: OAuthType,
    ): OAuthInfo {
        return this.oAuthProcessorFactory.getClient(oAuthType).getOAuth(accessToken)
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


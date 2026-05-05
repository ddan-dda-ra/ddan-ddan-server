package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.common.JWTTokenProvider
import notbe.tmtm.ddanddanserver.domain.exception.AdminInvalidCredentialsException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.MessageDigest

@Service
class AdminAuthService(
    @Value("\${admin.username}") private val configuredUsername: String,
    @Value("\${admin.password}") private val configuredPassword: String,
    private val jwtTokenProvider: JWTTokenProvider,
) {
    fun login(
        username: String,
        password: String,
    ): String {
        if (!constantTimeEquals(username, configuredUsername) ||
            !constantTimeEquals(password, configuredPassword)
        ) {
            throw AdminInvalidCredentialsException()
        }
        return jwtTokenProvider.createAdminAccessToken(username)
    }

    /**
     * 타이밍 공격을 막기 위한 상수 시간 비교.
     * 단순 == 비교는 첫 차이 위치에서 short-circuit하므로 응답 시간 차이로 비밀이 새어나갈 수 있다.
     */
    private fun constantTimeEquals(
        a: String,
        b: String,
    ): Boolean = MessageDigest.isEqual(a.toByteArray(), b.toByteArray())
}

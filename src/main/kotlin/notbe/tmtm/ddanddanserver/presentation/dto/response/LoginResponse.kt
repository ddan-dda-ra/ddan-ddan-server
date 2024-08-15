package notbe.tmtm.ddanddanserver.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import notbe.tmtm.ddanddanserver.domain.model.User

@Schema(description = "로그인 응답 DTO")
data class LoginResponse(
    @Schema(description = "엑세스 토큰", example = "ACCESS_TOKEN")
    val accessToken: String,
    @Schema(description = "리프레시 토큰", example = "REFRESH_TOKEN")
    val refreshToken: String,
    val user: UserResponse,
    @Schema(description = "회원 등록 여부")
    val isRegistered: Boolean,
) {
    companion object {
        fun fromDomain(
            accessToken: String,
            refreshToken: String,
            user: User,
            isRegistered: Boolean,
        ) = LoginResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = UserResponse.fromDomain(user),
            isRegistered = isRegistered,
        )
    }
}

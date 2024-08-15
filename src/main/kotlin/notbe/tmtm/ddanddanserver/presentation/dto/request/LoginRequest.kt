package notbe.tmtm.ddanddanserver.presentation.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType

@Schema(description = "OAuth 로그인 요청 DTO")
data class LoginRequest(
    @Schema(description = "OAuth 로그인 토큰", example = "KakaoToken")
    val token: String,
    @Schema(description = "OAuth 타입", example = "KAKAO")
    val tokenType: OAuthType,
)
